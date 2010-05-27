package org.fedorahosted.flies.rest.service;

import java.io.InputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.MessageBodyReader;

import org.apache.commons.lang.StringUtils;
import org.fedorahosted.flies.common.LocaleId;
import org.fedorahosted.flies.dao.DocumentDAO;
import org.fedorahosted.flies.dao.ProjectIterationDAO;
import org.fedorahosted.flies.model.HDocument;
import org.fedorahosted.flies.model.HProjectIteration;
import org.fedorahosted.flies.model.HTextFlow;
import org.fedorahosted.flies.model.po.HPoHeader;
import org.fedorahosted.flies.rest.LocaleIdSet;
import org.fedorahosted.flies.rest.NoSuchEntityException;
import org.fedorahosted.flies.rest.StringSet;
import org.fedorahosted.flies.rest.dto.v1.ResourcesList;
import org.fedorahosted.flies.rest.dto.v1.SourceResource;
import org.fedorahosted.flies.rest.dto.v1.SourceTextFlow;
import org.fedorahosted.flies.rest.dto.v1.TranslationResource;
import org.fedorahosted.flies.rest.dto.v1.ext.PoHeader;
import org.jboss.resteasy.util.HttpHeaderNames;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.security.Restrict;
import org.jboss.seam.resteasy.SeamResteasyProviderFactory;

import com.google.common.collect.ImmutableSet;

@Name("translationResourcesService")
@Path("/projects/p/{projectSlug}/iterations/i/{iterationSlug}/resources")
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class TranslationResourcesService {

	public static final Set<String> EXTENSIONS = ImmutableSet.of(PoHeader.ID);
	
	@PathParam("projectSlug")
	private String projectSlug;

	@PathParam("iterationSlug")
	private String iterationSlug;

	@QueryParam("ext") @DefaultValue("")
	private StringSet extensions;
	
	@HeaderParam("Content-Type")
	private MediaType requestContentType;

	@Context
	private UriInfo uriInfo;
	
	@Context
	private HttpHeaders headers;
	
	@In
	private ProjectIterationDAO projectIterationDAO;

	@In
	private DocumentDAO documentDAO;

	@In
	private DocumentUtils documentUtils;
	
	public TranslationResourcesService() {
	}
	
	public TranslationResourcesService(ProjectIterationDAO projectIterationDAO, DocumentDAO documentDAO, DocumentUtils documentUtils) {
		this.projectIterationDAO = projectIterationDAO;
		this.documentDAO = documentDAO;
		this.documentUtils = documentUtils;
	}
	
	/**
	 * Retrieve the List of Resources
	 *  
	 * @return Response.ok with ResourcesList or Response(404) if not found 
	 */
	@GET
	public Response doGet(
			@HeaderParam(HttpHeaderNames.IF_NONE_MATCH) EntityTag ifNoneMatch		
				) {
		
		HProjectIteration hProjectIteration = retrieveIteration();
		
		EntityTag etag = projectIterationDAO.getResourcesETag(hProjectIteration);

		if(etag == null)
			return Response.status(Status.NOT_FOUND).entity("document not found").build();

		if(ifNoneMatch != null && etag.equals(ifNoneMatch)) {
			return Response.notModified(ifNoneMatch).build();
		}
		
		
		ResourcesList resources = new ResourcesList();
		
		for(HDocument doc : hProjectIteration.getDocuments().values() ) {
			if(!doc.isObsolete()) {
				TranslationResource resource = new TranslationResource();
				documentUtils.transfer(doc, resource);
				resources.add(resource);
			}
		}

		return Response.ok().entity(resources).tag(etag).build();
		
	}
	
	@POST
	@Restrict("#{identity.loggedIn}")
	public Response doPost(InputStream messageBody) {

		HProjectIteration hProjectIteration = retrieveIteration();

		validateExtensions();

		SourceResource entity = unmarshallEntity(SourceResource.class, messageBody);
		RestUtils.validateEntity(entity);

		
		HDocument document = documentDAO.getByDocId(hProjectIteration, entity.getName()); 
		if(document != null) {
			if( !document.isObsolete() ) {
				// updates happens through PUT on the actual resource
				return Response.status(Status.CONFLICT)
					.entity("A document with name " + entity.getName() +" already exists.")
					.build();
			}
			// a deleted document is being created again 
			document.setObsolete(false);
		}
		else {
			document = new HDocument(entity.getName(),entity.getContentType());
			document.setProjectIteration(hProjectIteration);
		}
		
		documentUtils.transfer(entity, document);

		document = documentDAO.makePersistent(document);
		documentDAO.flush();

		// handle extensions
		
		// po header
		if ( documentUtils.transfer(entity.getExtensions(), document, extensions) ) {
			documentDAO.flush();
		}
		
		// TODO include extensions in etag generation
		EntityTag etag = documentDAO.getETag(hProjectIteration, document.getDocId(), extensions);
		
		return Response.created(URI.create("r/"+documentUtils.encodeDocId(document.getDocId())))
			.tag(etag).build();
	}

	@GET
	@Path("/r/{id}")
	public Response doResourceGet(
			@PathParam("id") String id, 
			@HeaderParam(HttpHeaderNames.IF_NONE_MATCH) EntityTag ifNoneMatch) {

		HProjectIteration hProjectIteration = retrieveIteration();

		validateExtensions();
		
		EntityTag etag = documentDAO.getETag(hProjectIteration, id, extensions);

		if(etag == null)
			return Response.status(Status.NOT_FOUND).entity("document not found").build();

		if(ifNoneMatch != null && etag.equals(ifNoneMatch)) {
			return Response.notModified(ifNoneMatch).build();
		}
		
		HDocument doc = documentDAO.getByDocId(hProjectIteration, id);

		if(doc == null) {
			return Response.status(Status.NOT_FOUND).entity("document not found").build();
		}

		SourceResource entity = new SourceResource(doc.getDocId());
		documentUtils.transfer(doc, entity);
		
		for(HTextFlow htf : doc.getTextFlows()) {
			SourceTextFlow tf = new SourceTextFlow(htf.getResId());
			documentUtils.transfer(htf, tf);
			entity.getTextFlows().add(tf);
		}

		// handle extensions
		documentUtils.transfer(doc, entity.getExtensions(), extensions);
		
		return Response.ok().entity(entity).tag(etag).lastModified(doc.getLastChanged()).build();
	}

	@PUT
	@Path("/r/{id}")
	@Restrict("#{identity.loggedIn}")
	public Response doResourcePut(
			@PathParam("id") String id, 
			@HeaderParam(HttpHeaderNames.IF_MATCH) EntityTag ifMatch, 
			InputStream messageBody) {

		HProjectIteration hProjectIteration = retrieveIteration();
		
		EntityTag etag = documentDAO.getETag(hProjectIteration, id, extensions);

		HDocument document;
		
		if(etag == null) {
			return Response.status(Status.NOT_FOUND).build();
		}
		else if(ifMatch != null && !etag.equals(ifMatch)) {
			return Response.status(Status.CONFLICT).build();
		}

		SourceResource entity = unmarshallEntity(SourceResource.class, messageBody);
		document = documentDAO.getByDocId(hProjectIteration, id);
		
		documentUtils.transfer(entity, document);
		
		// handle po header
		if( extensions.contains(PoHeader.ID) ) {
			PoHeader poHeaderExt = entity.getExtensions().findByType(PoHeader.class);
			if(poHeaderExt != null) {
				HPoHeader poHeader = document.getPoHeader(); 
				if ( poHeader == null) {
					poHeader = new HPoHeader();
					poHeader.setDocument(document);
					document.setPoHeader( poHeader );
					
				}
				documentUtils.transfer(poHeaderExt, poHeader);
			}
		}

		documentDAO.flush();
		etag = documentDAO.getETag(hProjectIteration, id, extensions);
		return Response.ok().tag(etag).build();
			
	}

	@DELETE
	@Path("/r/{id}")
	public Response doResourceDelete(			
			@PathParam("id") String id, 
			@HeaderParam(HttpHeaderNames.IF_MATCH) EntityTag ifMatch 
			) {
		HProjectIteration hProjectIteration = retrieveIteration();
		
		EntityTag etag = documentDAO.getETag(hProjectIteration, id, extensions);

		if(etag == null) {
			return Response.status(Status.NOT_FOUND).build();
		}

		if(ifMatch != null && !etag.equals(ifMatch)) {
			return Response.status(Status.CONFLICT).build();
		}

		HDocument document = documentDAO.getByDocId(hProjectIteration, id);
		document.setObsolete(true);
		documentDAO.flush();
		return Response.ok().build();
	}

	@GET
	@Path("/r/{id}/meta")
	public Response doResourceMetaGet(
			@PathParam("id") String id, 
			@HeaderParam(HttpHeaderNames.IF_NONE_MATCH) EntityTag ifNoneMatch) {
		
		HProjectIteration hProjectIteration = retrieveIteration();

		EntityTag etag = documentDAO.getETag(hProjectIteration, id, extensions);

		if(etag == null)
			return Response.status(Status.NOT_FOUND).entity("document not found").build();

		if(ifNoneMatch != null && etag.equals(ifNoneMatch)) {
			return Response.notModified(ifNoneMatch).build();
		}
		
		HDocument doc = documentDAO.getByDocId(hProjectIteration, id);

		if(doc == null) {
			return Response.status(Status.NOT_FOUND).entity("document not found").build();
		}

		TranslationResource entity = new TranslationResource(doc.getDocId());
		documentUtils.transfer(doc, entity);
		
		// transfer extensions
		documentUtils.transfer(doc, entity.getExtensions(), extensions);

		return Response.ok().entity(entity).tag(etag).build();
	}
	
	@PUT
	@Path("/r/{id}/meta")
	public Response doResourceMetaPut(
			@PathParam("id") String id, 
			@HeaderParam(HttpHeaderNames.IF_MATCH) EntityTag ifMatch, 
			InputStream messageBody) {

		HProjectIteration hProjectIteration = retrieveIteration();
		
		EntityTag etag = documentDAO.getETag(hProjectIteration, id, extensions);

		if(etag == null) {
			return Response.status(Status.NOT_FOUND).build();
		}
		else if(ifMatch != null && !etag.equals(ifMatch)) {
			return Response.status(Status.CONFLICT).build();
		}

		TranslationResource entity = unmarshallEntity(TranslationResource.class, messageBody);
		HDocument document = documentDAO.getByDocId(hProjectIteration, id);
		boolean changed = documentUtils.transfer(entity, document);
		
		// handle extensions
		changed |= documentUtils.transfer(entity.getExtensions(), document, extensions);
		
		if(changed) {
			documentDAO.flush();
			etag = documentDAO.getETag(hProjectIteration, id, extensions);
		}
		
		return Response.ok().tag(etag).lastModified(document.getLastChanged()).build();
			
	}
	
	
	@GET
	@Path("/r/{id}/target/{locale}")
	public Response doResourceTargetGet(
		@PathParam("id") String id,
		@PathParam("locale") LocaleIdSet locales,
		@HeaderParam(HttpHeaderNames.IF_NONE_MATCH) EntityTag ifNoneMatch) {

		return Response.ok().build();
	}
	
	@PUT
	@Path("/r/{id}/target/{locale}")
	public Response doResourceTargetPut(
		@PathParam("id") String id,
		@PathParam("locale") LocaleIdSet locales,
		@HeaderParam(HttpHeaderNames.IF_MATCH) EntityTag ifMatch) {

		return Response.ok().build();
	}

	@GET
	@Path("/r/{id}/target-as-source/{locale}")
	public Response doResourceTargetAsSourceGet(
		@PathParam("id") String id,
		@PathParam("locale") LocaleId locale,
		@HeaderParam(HttpHeaderNames.IF_NONE_MATCH) EntityTag ifNoneMatch) {

		return Response.ok().build();
	}
	
	private <T> T unmarshallEntity(Class<T> entityClass, InputStream is) {
		MessageBodyReader<T> reader = SeamResteasyProviderFactory.getInstance()
				.getMessageBodyReader(entityClass, entityClass,
						entityClass.getAnnotations(), requestContentType);
		if (reader == null) {
			throw new RuntimeException(
					"Unable to find MessageBodyReader for content type "
							+ requestContentType);
		}
		T entity;
		try {
			entity = reader.readFrom(entityClass, entityClass, entityClass
					.getAnnotations(), requestContentType, headers.getRequestHeaders(), is);
		} catch (Exception e) {
			throw new WebApplicationException(
					Response.status(Status.BAD_REQUEST).entity("Unable to read request body").build());
		}
		
		return entity;
	}
	
	private HProjectIteration retrieveIteration() {
		HProjectIteration hProjectIteration = projectIterationDAO.getBySlug(
				projectSlug, iterationSlug);

		if (hProjectIteration != null) {
			return hProjectIteration;
		}
		
		throw new NoSuchEntityException("Project Iteration '" + projectSlug+":"+iterationSlug+"' not found.");
	}

	private void validateExtensions() {
		Set<String> invalidExtensions = null;
		for(String ext : extensions) {
			if( ! EXTENSIONS.contains(ext)) {
				if(invalidExtensions == null) {
					invalidExtensions = new HashSet<String>();
				}
				invalidExtensions.add(ext);
			}
		}
		
		if(invalidExtensions != null) {
			throw new WebApplicationException(
					Response.status(Status.BAD_REQUEST)
						.entity("Unsupported Extensions: " + StringUtils.join(invalidExtensions, ",")).build());
			
		}
	}
}

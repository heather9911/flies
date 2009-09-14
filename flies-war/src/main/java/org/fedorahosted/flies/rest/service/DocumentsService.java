package org.fedorahosted.flies.rest.service;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.fedorahosted.flies.core.dao.DocumentDAO;
import org.fedorahosted.flies.core.dao.ProjectDAO;
import org.fedorahosted.flies.core.dao.ProjectIterationDAO;
import org.fedorahosted.flies.rest.MediaTypes;
import org.fedorahosted.flies.rest.dto.Documents;
import org.hibernate.Session;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.security.Restrict;

@Name("documentsService")
@Path("/projects/p/{projectSlug}/iterations/i/{iterationSlug}/documents")
public class DocumentsService {
	
	@PathParam("projectSlug")
	private String projectSlug;
	
	@PathParam("iterationSlug")
	private String iterationSlug;

	@In
	ProjectDAO projectDAO;
	
	@In
	ProjectIterationDAO projectIterationDAO;
	
	@In
	DocumentDAO documentDAO;
	
	@In
	Session session;

	@POST
	@Consumes({ MediaTypes.APPLICATION_FLIES_DOCUMENTS_XML, MediaType.APPLICATION_JSON })
	@Restrict("#{identity.loggedIn}")
	public Response post(Documents documents) {
	    return Response.ok().build();
	}

	@GET
	@Produces({ MediaTypes.APPLICATION_FLIES_DOCUMENTS_XML, MediaType.APPLICATION_JSON })
	public Documents get() {
	    return new Documents();
	}

	@PUT
	@Consumes({ MediaTypes.APPLICATION_FLIES_DOCUMENTS_XML, MediaType.APPLICATION_JSON })
	@Restrict("#{identity.loggedIn}")
	public Response put(Documents documents) {
	    return Response.ok().build();
	}
}
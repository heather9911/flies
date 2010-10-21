package net.openl10n.flies.rest.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import net.openl10n.flies.FliesRestTest;
import net.openl10n.flies.common.ContentState;
import net.openl10n.flies.common.ContentType;
import net.openl10n.flies.common.LocaleId;
import net.openl10n.flies.common.ResourceType;
import net.openl10n.flies.dao.DocumentDAO;
import net.openl10n.flies.dao.PersonDAO;
import net.openl10n.flies.dao.ProjectIterationDAO;
import net.openl10n.flies.dao.LocaleDAO;
import net.openl10n.flies.dao.TextFlowTargetDAO;
import net.openl10n.flies.rest.client.ITranslationResources;
import net.openl10n.flies.rest.dto.Person;
import net.openl10n.flies.rest.dto.resource.Resource;
import net.openl10n.flies.rest.dto.resource.ResourceMeta;
import net.openl10n.flies.rest.dto.resource.TextFlow;
import net.openl10n.flies.rest.dto.resource.TextFlowTarget;
import net.openl10n.flies.rest.dto.resource.TranslationsResource;
import net.openl10n.flies.service.impl.LocaleServiceImpl;

import org.apache.commons.httpclient.URIException;
import org.dbunit.operation.DatabaseOperation;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.seam.security.Identity;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TranslationResourceRestTest extends FliesRestTest
{

   private final String RESOURCE_PATH = "/projects/p/sample-project/iterations/i/1.0/r/";

   IMocksControl mockControl = EasyMock.createControl();
   Identity mockIdentity = mockControl.createMock(Identity.class);

   @BeforeClass
   void beforeClass()
   {
      Identity.setSecurityEnabled(false);
   }

   @BeforeMethod
   void reset()
   {
      mockControl.reset();
   }

   @Override
   protected void prepareDBUnitOperations()
   {
      beforeTestOperations.add(new DataSetOperation("META-INF/testdata/ProjectsData.dbunit.xml", DatabaseOperation.CLEAN_INSERT));
      beforeTestOperations.add(new DataSetOperation("META-INF/testdata/LocalesData.dbunit.xml", DatabaseOperation.CLEAN_INSERT));
   }

   @Override
   protected void prepareResources()
   {
      final ProjectIterationDAO projectIterationDAO = new ProjectIterationDAO(getSession());
      final DocumentDAO documentDAO = new DocumentDAO(getSession());
      final PersonDAO personDAO = new PersonDAO(getSession());
      final TextFlowTargetDAO textFlowTargetDAO = new TextFlowTargetDAO(getSession());
      final ResourceUtils resourceUtils = new ResourceUtils();
      final ETagUtils eTagUtils = new ETagUtils(getSession(), documentDAO);

      LocaleServiceImpl localeService = new LocaleServiceImpl();
      LocaleDAO localeDAO = new LocaleDAO(getSession());
      localeService.setLocaleDAO(localeDAO);
      TranslationResourcesService obj = new TranslationResourcesService(projectIterationDAO, documentDAO, personDAO, textFlowTargetDAO, localeService, resourceUtils, mockIdentity, eTagUtils);

      resources.add(obj);
   }

   @Test
   public void fetchEmptyListOfResources()
   {
      doGetandAssertThatResourceListContainsNItems(0);
   }

   @Test
   public void createEmptyResource()
   {
      ITranslationResources client = getClientRequestFactory().createProxy(ITranslationResources.class, createBaseURI(RESOURCE_PATH));

      Resource sr = createSourceResource("my.txt");

      ClientResponse<String> response = client.post(sr, null);
      assertThat(response.getResponseStatus(), is(Status.CREATED));
      List<String> locationHeader = response.getHeaders().get("Location");
      assertThat(locationHeader.size(), is(1));
      assertThat(locationHeader.get(0), endsWith("r/my.txt"));
      doGetandAssertThatResourceListContainsNItems(1);
   }

   @Test
   public void createResourceWithContentUsingPost()
   {
      ITranslationResources client = getClientRequestFactory().createProxy(ITranslationResources.class, createBaseURI(RESOURCE_PATH));

      Resource sr = createSourceResource("my.txt");

      TextFlow stf = new TextFlow("tf1", LocaleId.EN, "tf1");
      sr.getTextFlows().add(stf);

      ClientResponse<String> postResponse = client.post(sr, null);
      assertThat(postResponse.getResponseStatus(), is(Status.CREATED));
      postResponse = client.post(sr, null);

      ClientResponse<Resource> resourceGetResponse = client.getResource("my.txt", null);
      assertThat(resourceGetResponse.getResponseStatus(), is(Status.OK));
      Resource gotSr = resourceGetResponse.getEntity();
      assertThat(gotSr.getTextFlows().size(), is(1));
      assertThat(gotSr.getTextFlows().get(0).getContent(), is("tf1"));

   }

   @Test
   public void createResourceWithContentUsingPut()
   {
      ITranslationResources client = getClientRequestFactory().createProxy(ITranslationResources.class, createBaseURI(RESOURCE_PATH));

      Resource sr = createSourceResource("my.txt");

      TextFlow stf = new TextFlow("tf1", LocaleId.EN, "tf1");
      sr.getTextFlows().add(stf);

      ClientResponse<String> response = client.putResource("my.txt", sr, null);
      assertThat(response.getResponseStatus(), is(Status.CREATED));
      assertThat(response.getLocation().getHref(), endsWith("/r/my.txt"));

      ClientResponse<Resource> resourceGetResponse = client.getResource("my.txt", null);
      assertThat(resourceGetResponse.getResponseStatus(), is(Status.OK));
      Resource gotSr = resourceGetResponse.getEntity();
      assertThat(gotSr.getTextFlows().size(), is(1));
      assertThat(gotSr.getTextFlows().get(0).getContent(), is("tf1"));

   }

   @Test
   public void createPoResourceWithPoHeader()
   {
      ITranslationResources client = getClientRequestFactory().createProxy(ITranslationResources.class, createBaseURI(RESOURCE_PATH));

      Resource sr = createSourceResource("my.txt");

      TextFlow stf = new TextFlow("tf1", LocaleId.EN, "tf1");
      sr.getTextFlows().add(stf);

      // @formatter:off
      /*
      TODO: move this into an AbstractResourceMeta test (PoHeader is valid for source documents, not target)

      PoHeader poHeaderExt = new PoHeader("comment", new HeaderEntry("h1", "v1"), new HeaderEntry("h2", "v2"));
      sr.getExtensions(true).add(poHeaderExt);
      
      */
      // @formatter:on

      ClientResponse<String> postResponse = client.post(sr, null); // new
                                                                   // StringSet(PoHeader.ID));
      assertThat(postResponse.getResponseStatus(), is(Status.CREATED));
      doGetandAssertThatResourceListContainsNItems(1);

      ClientResponse<Resource> resourceGetResponse = client.getResource("my.txt", null);// new
                                                                                        // StringSet(PoHeader.ID));
      assertThat(resourceGetResponse.getResponseStatus(), is(Status.OK));
      Resource gotSr = resourceGetResponse.getEntity();
      assertThat(gotSr.getTextFlows().size(), is(1));
      assertThat(gotSr.getTextFlows().get(0).getContent(), is("tf1"));

      // @formatter:off
      /*
      TODO: move this into an AbstractResourceMeta test

      assertThat(gotSr.getExtensions().size(), is(1));
      PoHeader gotPoHeader = gotSr.getExtensions().findByType(PoHeader.class);
      assertThat(gotPoHeader, notNullValue());
      assertThat(poHeaderExt.getComment(), is(gotPoHeader.getComment()));
      assertThat(poHeaderExt.getEntries(), is(gotPoHeader.getEntries()));
      */
      // @formatter:on
   }

   // NB this test breaks in Maven if the dev profile is active (because of the
   // imported testdata)
   public void publishTranslations()
   {
      createResourceWithContentUsingPut();

      ITranslationResources client = getClientRequestFactory().createProxy(ITranslationResources.class, createBaseURI(RESOURCE_PATH));

      TranslationsResource entity = new TranslationsResource();
      TextFlowTarget target = new TextFlowTarget("tf1");
      target.setContent("hello world");
      target.setState(ContentState.Approved);
      target.setTranslator(new Person("root@localhost", "Admin user"));
      entity.getTextFlowTargets().add(target);

      LocaleId de_DE = new LocaleId("de");
      ClientResponse<String> response = client.putTranslations("my.txt", de_DE, entity, null);

      assertThat(response.getResponseStatus(), is(Status.OK));

      ClientResponse<TranslationsResource> getResponse = client.getTranslations("my.txt", de_DE, null);
      assertThat(getResponse.getResponseStatus(), is(Status.OK));
      TranslationsResource entity2 = getResponse.getEntity();
      assertThat(entity2.getTextFlowTargets().size(), is(entity.getTextFlowTargets().size()));

      entity.getTextFlowTargets().clear();
      response = client.putTranslations("my.txt", de_DE, entity, null);

      assertThat(response.getResponseStatus(), is(Status.OK));

      getResponse = client.getTranslations("my.txt", de_DE, null);
      // TODO this should return an empty set of targets, possibly with metadata
      assertThat(getResponse.getResponseStatus(), is(Status.NOT_FOUND));

   }

   public void getDocumentThatDoesntExist()
   {
      ITranslationResources transResource = getClientRequestFactory().createProxy(ITranslationResources.class, createBaseURI(RESOURCE_PATH));
      ClientResponse<Resource> clientResponse = transResource.getResource("my,doc,does,not,exist.txt", null);
      assertThat(clientResponse.getResponseStatus(), is(Status.NOT_FOUND));
   }

   public void getDocument() throws URIException
   {
      // NB the new rest API does not map '/' to ','
      // if a document is PUT with a '/' in the docId, there is no
      // way to GET it back.
      String docUri = "my,path,document.txt";
      ITranslationResources transResource = getClientRequestFactory().createProxy(ITranslationResources.class, createBaseURI(RESOURCE_PATH));
      Resource resource = createSourceDoc(docUri);
      transResource.putResource(docUri, resource, null);

      ClientResponse<ResourceMeta> response = transResource.getResourceMeta(docUri, null);
      assertThat(response.getResponseStatus(), is(Status.OK));
      ResourceMeta doc = response.getEntity();
      assertThat(doc.getName(), is(docUri));
      assertThat(doc.getContentType(), is(ContentType.TextPlain));
      assertThat(doc.getLang(), is(LocaleId.EN_US));
      // FIXME check revision!
      // assertThat( doc.getRevision(), is(1) );

      /*
       * Link link = doc.getLinks().findLinkByRel(Relationships.SELF);
       * assertThat( link, notNullValue() ); assertThat(
       * URIUtil.decode(link.getHref().toString()), endsWith(url+docUri) );
       * 
       * link = doc.getLinks().findLinkByRel(Relationships.DOCUMENT_CONTAINER);
       * assertThat( link, notNullValue() ); assertThat(
       * link.getHref().toString(), endsWith("iterations/i/1.0") );
       */
   }

   public void getDocumentWithResources() throws URIException
   {
      LocaleId nbLocale = new LocaleId("de");
      String docUri = "my,path,document.txt";
      ITranslationResources transResource = getClientRequestFactory().createProxy(ITranslationResources.class, createBaseURI(RESOURCE_PATH));
      Resource resource = createSourceDoc(docUri);
      transResource.putResource(docUri, resource, null);
      TranslationsResource trans = createTargetDoc();
      transResource.putTranslations(docUri, nbLocale, trans, null);

      {
         ClientResponse<Resource> response = transResource.getResource(docUri, null);
         assertThat(response.getResponseStatus(), is(Status.OK));

         Resource doc = response.getEntity();
         assertThat(doc.getTextFlows().size(), is(1));
      }

      ClientResponse<TranslationsResource> response = transResource.getTranslations(docUri, nbLocale, null);
      assertThat(response.getResponseStatus(), is(Status.OK));

      TranslationsResource doc = response.getEntity();
      assertThat("should have one textFlow", doc.getTextFlowTargets().size(), is(1));
      TextFlowTarget tft = doc.getTextFlowTargets().get(0);

      assertThat(tft, notNullValue());
      assertThat("should have a textflow with this id", tft.getResId(), is("tf1"));

      assertThat("expected de target", tft, notNullValue());
      assertThat("expected translation for de", tft.getContent(), is("hei verden"));

   }

   public void putNewDocument()
   {
      // NB the new rest API does not map '/' to ','
      String docUrl = "my,fancy,document.txt";
      ITranslationResources transResource = getClientRequestFactory().createProxy(ITranslationResources.class, createBaseURI(RESOURCE_PATH));
      Resource doc = createSourceDoc(docUrl);
      Response response = transResource.putResource(docUrl, doc, null);

      assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
      assertThat(response.getMetadata().getFirst("Location").toString(), endsWith(RESOURCE_PATH + docUrl));

      ClientResponse<Resource> documentResponse = transResource.getResource(docUrl, null);

      assertThat(documentResponse.getResponseStatus(), is(Status.OK));

      doc = documentResponse.getEntity();
      // FIXME check revision!
      // assertThat(doc.getRevision(), is(1));

      /*
       * Link link = doc.getLinks().findLinkByRel(Relationships.SELF);
       * assertThat(link, notNullValue()); assertThat(link.getHref().toString(),
       * endsWith(url + docUrl));
       * 
       * link = doc.getLinks().findLinkByRel(Relationships.DOCUMENT_CONTAINER);
       * assertThat(link, notNullValue()); assertThat(link.getType(),
       * is(MediaTypes.APPLICATION_FLIES_PROJECT_ITERATION_XML));
       */
   }

   public void putNewDocumentWithResources() throws Exception
   {
      // NB the new rest API does not map '/' to ','
      String docUrl = "my,fancy,document.txt";
      ITranslationResources transResource = getClientRequestFactory().createProxy(ITranslationResources.class, createBaseURI(RESOURCE_PATH));
      Resource doc = createSourceDoc(docUrl);

      List<TextFlow> textFlows = doc.getTextFlows();
      textFlows.clear();

      TextFlow textFlow = new TextFlow("tf1");
      textFlow.setContent("hello world!");
      textFlows.add(textFlow);

      TextFlow tf3 = new TextFlow("tf3");
      tf3.setContent("more text");
      textFlows.add(tf3);

      Marshaller m = null;
      JAXBContext jc = JAXBContext.newInstance(Resource.class);
      m = jc.createMarshaller();
      m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      m.marshal(doc, System.out);

      Response response = transResource.putResource(docUrl, doc, null);

      assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
      assertThat(response.getMetadata().getFirst("Location").toString(), endsWith(RESOURCE_PATH + docUrl));

      ClientResponse<Resource> documentResponse = transResource.getResource(docUrl, null);

      assertThat(documentResponse.getResponseStatus(), is(Status.OK));

      doc = documentResponse.getEntity();

      // FIXME check revision!
      // assertThat(doc.getRevision(), is(1));

      assertThat("Should have textFlows", doc.getTextFlows(), notNullValue());
      assertThat("Should have 2 textFlows", doc.getTextFlows().size(), is(2));
      assertThat("Should have tf1 textFlow", doc.getTextFlows().get(0).getId(), is("tf1"));
      assertThat("Container1 should have tf3 textFlow", doc.getTextFlows().get(1).getId(), is(tf3.getId()));

      textFlow = doc.getTextFlows().get(0);
      textFlow.setId("tf2");

      response = transResource.putResource(docUrl, doc, null);

      // TODO this WAS testing for status 205
      assertThat(response.getStatus(), is(200));

      documentResponse = transResource.getResource(docUrl, null);
      assertThat(documentResponse.getResponseStatus(), is(Status.OK));
      doc = documentResponse.getEntity();

      // FIXME check revision!
      // assertThat(doc.getRevision(), is(2));

      assertThat("Should have textFlows", doc.getTextFlows(), notNullValue());
      assertThat("Should have two textFlows", doc.getTextFlows().size(), is(2));
      assertThat("should have same id", doc.getTextFlows().get(0).getId(), is("tf2"));
   }

   // END of tests

   private Resource createSourceDoc(String name)
   {
      Resource resource = new Resource();
      resource.setContentType(ContentType.TextPlain);
      resource.setLang(LocaleId.EN_US);
      resource.setName(name);
      resource.setType(ResourceType.DOCUMENT);

      resource.getTextFlows().add(new TextFlow("tf1", LocaleId.EN_US, "hello world"));
      return resource;
   }

   private TranslationsResource createTargetDoc()
   {
      TranslationsResource trans = new TranslationsResource();
      TextFlowTarget target = new TextFlowTarget();
      target.setContent("hei verden");
      target.setDescription("translation of hello world");
      target.setResId("tf1");
      target.setState(ContentState.Approved);
      Person person = new Person("email@example.com", "Translator Name");
      target.setTranslator(person);
      trans.getTextFlowTargets().add(target);
      return trans;
   }

   private Resource createSourceResource(String name)
   {
      Resource sr = new Resource(name);
      sr.setContentType(ContentType.TextPlain);
      sr.setLang(LocaleId.EN);
      sr.setType(ResourceType.FILE);
      return sr;
   }

   private void doGetandAssertThatResourceListContainsNItems(int n)
   {
      ITranslationResources client = getClientRequestFactory().createProxy(ITranslationResources.class, createBaseURI(RESOURCE_PATH));
      ClientResponse<List<ResourceMeta>> resources = client.get(null);
      assertThat(resources.getResponseStatus(), is(Status.OK));

      assertThat(resources.getEntity().size(), is(n));
   }

}
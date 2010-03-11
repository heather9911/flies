package org.fedorahosted.flies.client.ant.po;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.cyclopsgroup.jcli.ArgumentProcessor;
import org.cyclopsgroup.jcli.annotation.Argument;
import org.cyclopsgroup.jcli.annotation.Cli;
import org.cyclopsgroup.jcli.annotation.MultiValue;
import org.cyclopsgroup.jcli.annotation.Option;
import org.fedorahosted.flies.adapter.po.PoReader;
import org.fedorahosted.flies.common.ContentType;
import org.fedorahosted.flies.common.LocaleId;
import org.fedorahosted.flies.rest.ClientUtility;
import org.fedorahosted.flies.rest.FliesClientRequestFactory;
import org.fedorahosted.flies.rest.client.IDocumentsResource;
import org.fedorahosted.flies.rest.dto.Document;
import org.fedorahosted.flies.rest.dto.Documents;
import org.jboss.resteasy.client.ClientResponse;
import org.xml.sax.InputSource;

@Cli(name = "uploadpo", description = "Uploads a Publican project's PO/POT files to Flies for translation")
public class UploadPoTask extends MatchingTask {

	private String user;
	private String apiKey;
	private String dst;
	private File srcDir;
	private String sourceLang = "en_US";
	private boolean debug;
	private boolean help;

	public static void main(String[] args) throws Exception {
		UploadPoTask upload = new UploadPoTask();
		ArgumentProcessor<UploadPoTask> argProcessor = ArgumentProcessor.newInstance(UploadPoTask.class);
		argProcessor.process(args, upload);

		if (upload.help)
			help(argProcessor);
		if (upload.srcDir == null)
			missingOption("--src");
			
		upload.execute();
	}
	
	private static void missingOption(String name) {
		System.out.println("Required option missing: "+name);
		System.exit(1);
	}

	private static void help(ArgumentProcessor<?> argProcessor)
			throws IOException {
		final PrintWriter out = new PrintWriter(System.out);
		argProcessor.printHelp(out);
		out.flush();
		System.exit(0);
	}
	
	private List<String> arguments = new ArrayList<String>();
	 
	@MultiValue
	@Argument( description = "Left over arguments" )
	public List<String> getArguments() { return arguments; }

	public void setArguments(List<String> arguments) {
		this.arguments = arguments;
	}
	
	public void execute() throws BuildException {
		ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
		try {
			// make sure RESTEasy classes will be found:
			Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
			process();
		} catch (Exception e) {
			throw new BuildException(e);
		} finally {
			Thread.currentThread().setContextClassLoader(oldLoader);
		}
	}
	
	public void process() throws JAXBException, MalformedURLException, URISyntaxException {
//		Document doc = new Document("doc1","mydoc.doc", "/", PoReader.PO_CONTENT_TYPE);
		
//			InputSource inputSource = new InputSource("file:/home/cchance/src/fedorahosted/flies/flies-client/flies-client-ant-po/src/test/resources/test-input/ja-JP/Accounts_And_Subscriptions.po");
//			
//			inputSource.setEncoding("utf8");
//			
			PoReader poReader = new PoReader();
//
//			System.out.println("parsing template");
//			poReader.extractTemplate(doc, inputSource);
			
			// scan the directory for pot files
			File potDir = new File(srcDir, "pot");
			File[] potFiles = potDir.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					return pathname.isFile() && pathname.getName().endsWith(".pot");
				}
			});
			
			// debug: print scanned files
			if (debug) {
				System.out.println("Here are scanned files: ");
				for (File potFile : potFiles)
					System.out.println("  "+potFile);
			}

			JAXBContext jc = JAXBContext.newInstance(Documents.class);
			Marshaller m = jc.createMarshaller();
			
			// debug
			if (debug)
				m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

			Documents docs = new Documents();
			List<Document> docList = docs.getDocuments();
			
			File[] localeDirs = srcDir.listFiles(new FileFilter() {
				@Override
				public boolean accept(File f) {
					return f.isDirectory() && !f.getName().equals("pot");
				}
			});
			
			// for each of the base pot files under srcdir/pot:
			for (File potFile : potFiles) {
//				progress.update(i++, files.length);
//				File sourceLang = new File(srcDir, locale);
				String basename = StringUtil.removeFileExtension(potFile.getName(), ".pot");
				Document doc = new Document(basename, ContentType.TextPlain);
				InputSource potInputSource = new InputSource(potFile.toURI().toString());
				System.out.println(potFile.toURI().toString());
				potInputSource.setEncoding("utf8");
				poReader.extractTemplate(doc, potInputSource, LocaleId.fromJavaName(sourceLang));
				docList.add(doc);
				
				String poName = basename + ".po";
				
				// for each of the corresponding po files in the locale subdirs:
				for (int i = 0; i < localeDirs.length; i++) {
					File localeDir = localeDirs[i];
					File poFile = new File(localeDir, poName);
					if (poFile.exists()) {
	//					progress.update(i++, files.length);
						InputSource inputSource = new InputSource(poFile.toURI().toString());
						System.out.println(poFile.toURI().toString());
						inputSource.setEncoding("utf8");
						poReader.extractTarget(doc, inputSource, new LocaleId(localeDir.getName()));
					}
				}
			}		
//			progress.finished();
			
			if (debug) {
				m.marshal(docs, System.out);
			}

			if(dst == null)
				return;

			// check if local or remote: write to file if local, put to server if remote
			URL dstURL = Utility.createURL(dst, getProject());
			if("file".equals(dstURL.getProtocol())) {
				m.marshal(docs, new File(dstURL.getFile()));
			} else {
				// send project to rest api
				FliesClientRequestFactory factory = new FliesClientRequestFactory(user, apiKey);
				IDocumentsResource documentsResource = factory.getDocumentsResource(dstURL.toURI());
				ClientResponse response = documentsResource.put(docs);
				ClientUtility.checkResult(response, dstURL);
			}
	}
	
	@Override
	public void log(String msg) {
		super.log(msg+"\n\n");
	}
	
	public void setUser(String user) {
		this.user = user;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public void setDst(String dst) {
		this.dst = dst;
	}
	
	// NB options whose longNames with "-" never get set
	@Option(name = "s", longName = "src", required = true, description = "Base directory for publican files")
	public void setSrcDir(File srcDir) {
		this.srcDir = srcDir;
	}

	public void setSourceLang(String sourceLang) {
		this.sourceLang = sourceLang;
	}

	@Option(name = "d", longName = "debug", description = "Enable debug mode")
	public void setDebug(boolean debug) {
		this.debug = debug;
	}
	
	@Option(name = "h", longName = "help", description = "Display this help and exit")
	public void setHelp(boolean help) {
		this.help = help;
	}
}

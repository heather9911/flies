package org.fedorahosted.flies.rest;

import java.io.IOException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.interception.MessageBodyWriterContext;
import org.jboss.resteasy.spi.interception.MessageBodyWriterInterceptor;
import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;

@Provider
@ServerInterceptor
public class JsonPWriterInterceptor implements MessageBodyWriterInterceptor
{

   @Context
   HttpRequest request;

   private static final Log log = Logging.getLog(JsonPWriterInterceptor.class);

   @Override
   public void write(MessageBodyWriterContext context) throws IOException, WebApplicationException
   {
      String jsonp = request.getUri().getQueryParameters().getFirst("jsonp");
      if (jsonp != null)
      {
         context.getOutputStream().write(jsonp.getBytes());
         context.getOutputStream().write("{".getBytes());
         context.proceed();
         context.getOutputStream().write("}".getBytes());
      }
      else
      {
         context.proceed();
      }
   }

}

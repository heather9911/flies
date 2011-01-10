package net.openl10n.flies.client;

import java.io.PrintStream;
import java.util.Collection;

import net.openl10n.flies.client.commands.BasicOptions;
import net.openl10n.flies.client.commands.RuntimeExceptionStrategy;

import org.codehaus.plexus.util.StringOutputStream;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class FliesClientTest
{
   private static final Logger log = LoggerFactory.getLogger(FliesClientTest.class);
   StringOutputStream out;
   StringOutputStream err;
   FliesClient client;

   @BeforeMethod
   void before()
   {
      out = new StringOutputStream();
      err = new StringOutputStream();
      client = new FliesClient(new RuntimeExceptionStrategy(), new PrintStream(out), new PrintStream(err));
   }

   @AfterMethod
   void after()
   {
      String outOutput = out.toString();
      if (!outOutput.isEmpty())
         log.debug("stdout:\n{}", outOutput);
      String errOutput = err.toString();
      if (!errOutput.isEmpty())
         log.debug("stderr:\n{}", errOutput);
   }

   @DataProvider(name = "options")
   public static Object[][] createOptions() throws Exception
   {
      return toGrid(new FliesClient().getOptionsMap().values());
   }

   /**
    * Useful for TestNG DataProvider methods.
    * 
    * @param collection
    * @return
    * @throws Exception
    */
   private static Object[][] toGrid(Collection<?> collection) throws Exception
   {
      Object[][] result = new Object[collection.size()][1];
      int i = 0;
      for (Object obj : collection)
      {
         result[i++] = new Object[] { obj };
      }
      return result;
   }

   @Test(dataProvider = "options")
   public void testForAmbiguousOptions(BasicOptions cmd) throws Exception
   {
      new CmdLineParser(cmd);
   }

   @Test(dataProvider = "options")
   public void testHelpCommand(BasicOptions cmd) throws Exception
   {
      client.processArgs("help", cmd.getCommandName());
   }

   @Test(dataProvider = "options")
   public void testHelpOption(BasicOptions cmd) throws Exception
   {
      client.processArgs(cmd.getCommandName(), "--help");
   }

   @Test(dataProvider = "options")
   public void testNoArgs(BasicOptions cmd) throws Exception
   {
      client.processArgs(cmd.getCommandName());
   }
}

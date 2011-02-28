package net.openl10n.flies.client.commands;

public class SystemExitStrategy implements AppAbortStrategy
{
   @Override
   public void abort(Throwable e)
   {
      System.exit(1);
   }
}
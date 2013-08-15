package com.voxeo.moho.common.util;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

public class InheritLogContextThreadPoolExecutor extends ScheduledThreadPoolExecutor {
  private static final Logger LOG = Logger.getLogger(InheritLogContextThreadPoolExecutor.class);

  public InheritLogContextThreadPoolExecutor(int corePoolSize, long keepAliveTime, TimeUnit unit,
      ThreadFactory threadFactory) {
    super(corePoolSize, threadFactory);
    super.setKeepAliveTime(keepAliveTime, unit);
  }

  @Override
  public void execute(Runnable command) {
    super.execute(command);
  }

  @Override
  protected void beforeExecute(Thread t, Runnable r) {
    if (r instanceof InheritLogContextI) {
      Utils.inheritLogContexts(((InheritLogContextI) r).getLogContexts());
    }
    super.beforeExecute(t, r);
  }

  @Override
  protected void afterExecute(Runnable r, Throwable t) {
    Utils.clearContexts();
    if (t != null) {
      LOG.error("Exception when executing " + r, t);
    }
    super.afterExecute(r, t);
  }
}

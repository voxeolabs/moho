package com.voxeo.moho.common.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class InheritLogContextThreadPoolExecutor extends ThreadPoolExecutor {

  public InheritLogContextThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
      BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
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
    super.afterExecute(r, t);
  }
}

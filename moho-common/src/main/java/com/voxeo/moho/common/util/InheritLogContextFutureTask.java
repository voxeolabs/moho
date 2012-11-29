package com.voxeo.moho.common.util;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class InheritLogContextFutureTask<V> extends FutureTask<V> implements InheritLogContextI {

  final Map<String, String> logContexts = Utils.getCurrentLogContexts();

  public Map<String, String> getLogContexts() {
    return logContexts;
  }

  public InheritLogContextFutureTask(Callable<V> callable) {
    super(callable);
  }

  public InheritLogContextFutureTask(Runnable runnable, V result) {
    super(runnable, result);
  }

}

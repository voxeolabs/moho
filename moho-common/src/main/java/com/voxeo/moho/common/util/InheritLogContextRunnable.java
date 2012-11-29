package com.voxeo.moho.common.util;

import java.util.Map;

public abstract class InheritLogContextRunnable implements Runnable, InheritLogContextI {

  final Map<String, String> logContexts = Utils.getCurrentLogContexts();

  public Map<String, String> getLogContexts() {
    return logContexts;
  }

}

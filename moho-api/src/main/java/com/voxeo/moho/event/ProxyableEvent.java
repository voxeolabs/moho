package com.voxeo.moho.event;

import com.voxeo.moho.Endpoint;
import com.voxeo.moho.SignalException;

public interface ProxyableEvent {
  boolean isProxied();
  
  void proxyTo(boolean recordRoute, boolean parallel, Endpoint... destinations) throws SignalException;
}

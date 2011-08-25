package com.voxeo.moho.event;

import java.util.Map;

import com.voxeo.moho.Endpoint;
import com.voxeo.moho.SignalException;

public interface ProxyableEvent {
  boolean isProxied();
  
  void proxyTo(boolean recordRoute, boolean parallel, Endpoint... destinations) throws SignalException;
  
  void proxyTo(boolean recordRoute, boolean parallel, final Map<String, String> headers, Endpoint... destinations);
}

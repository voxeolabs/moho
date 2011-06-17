package com.voxeo.moho.spi;

import com.voxeo.moho.Application;
import com.voxeo.moho.Framework;

public interface SpiFramework extends Framework {
  void registerDriver(String protocol, String className) throws ClassNotFoundException, InstantiationException, IllegalAccessException;
  String[] getProtocolFamilies();
  String[] getEndpointSchemas();
  ProtocolDriver getDriverByProtocolFamily(String protocol);
  ProtocolDriver getDriverByEndpointSechma(String schema);
  Application getApplication();
  ExecutionContext getExecutionContext();
}

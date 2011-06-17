package com.voxeo.moho.spi;

import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Endpoint;

public interface TextProvider {

  public String getType();

  public Endpoint createEndpoint(String uri, ApplicationContext ctx);
}

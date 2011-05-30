package com.voxeo.moho.textchannel;

import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Endpoint;

public interface TextChannelProvider {

  public String getType();

  public Endpoint createEndpoint(String uri, ApplicationContext ctx);
}

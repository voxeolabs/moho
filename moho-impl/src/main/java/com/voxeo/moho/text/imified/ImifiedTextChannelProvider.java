package com.voxeo.moho.text.imified;

import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.textchannel.TextChannelProvider;

public class ImifiedTextChannelProvider implements TextChannelProvider {

  @Override
  public String getType() {
    return "imfied";
  }

  @Override
  public Endpoint createEndpoint(String uri, ApplicationContext ctx) {
    if (uri.startsWith("im:")) {
      return new ImifiedEndpointImpl(ctx, uri.substring(uri.indexOf(":") + 1));
    }
    else {
      throw new IllegalArgumentException("");
    }
  }
}

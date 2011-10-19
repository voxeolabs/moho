package com.voxeo.moho.remote;

import java.net.URI;

import com.voxeo.moho.ApplicationContextImpl;
import com.voxeo.moho.Endpoint;

public class RemoteEndpointImpl implements Endpoint {

  protected ApplicationContextImpl _ctx;

  // call id, or conference id, or dialog id.
  protected String _id;

  public RemoteEndpointImpl(ApplicationContextImpl ctx, String id) {
    _ctx = ctx;
    _id = id;
  }

  @Override
  public String getName() {
    return _id;
  }

  @Override
  public URI getURI() {
    return URI.create(_id);
  }
}

package com.voxeo.moho.presence.impl;


import org.apache.log4j.Logger;

import com.voxeo.moho.presence.PresenceStore;
import com.voxeo.moho.presence.Resource;
import com.voxeo.moho.spi.ExecutionContext;

@SuppressWarnings("serial")
public abstract class AbstractResource implements Resource {
  
  private static final Logger LOG = Logger.getLogger(AbstractResource.class);
  
  protected transient ExecutionContext _context;
  
  private String _uri;
  
  public AbstractResource(ExecutionContext context, String resourceUri) {
    _context = context;
    _uri = resourceUri;
  }
  
  protected PresenceStore getStore() {
    return (PresenceStore) StoreHolder.getPresenceStore();
  }

  @Override
  public void setExecutionContext(ExecutionContext context) {
    _context = context;
  }
  
  @Override
  public String getUri() {
    return _uri;
  }

  public Resource clone() {
    try {
      return (Resource) super.clone();
    }
    catch (CloneNotSupportedException e) {
      LOG.error("Clone error for " + this, e);
    }
    return null;
  }
}

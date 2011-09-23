package com.voxeo.moho.presence.sip.impl;

import org.apache.log4j.Logger;

import com.voxeo.moho.presence.NotifyBody;
import com.voxeo.moho.presence.PresenceStore;
import com.voxeo.moho.presence.impl.AbstractResource;
import com.voxeo.moho.presence.impl.StoreHolder;
import com.voxeo.moho.presence.sip.SIPResource;
import com.voxeo.moho.spi.ExecutionContext;

public abstract class AbstractSIPResource extends AbstractResource implements SIPResource {
  
  protected static final Logger LOG = Logger.getLogger(AbstractSIPResource.class);
  
  private final String _eventName;

  public AbstractSIPResource(ExecutionContext context, String resourceUri, String eventName) {
    super(context, resourceUri);
    _eventName = eventName;
  }

  public String getEventName() {
    return _eventName;
  }
  
  @Override
  public NotifyBody getNotifyBody(String notifyBodyType) {
    PresenceStore presenceStore = StoreHolder.getPresenceStore();
    NotifyBody notifyBody = presenceStore.getNotifyBody(getUri(), _eventName, notifyBodyType);
    if (notifyBody == null) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Can't find notify body for " + this + ", use neutral body");
      }
      return createNeutralBody();
    }
    return notifyBody;
  }

  protected abstract NotifyBody createNeutralBody();
  
}

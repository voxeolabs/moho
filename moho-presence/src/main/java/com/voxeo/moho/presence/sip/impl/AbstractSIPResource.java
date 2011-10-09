package com.voxeo.moho.presence.sip.impl;

import org.apache.log4j.Logger;

import com.voxeo.moho.event.SubscribeEvent.SubscriptionContext;
import com.voxeo.moho.presence.NotifyBody;
import com.voxeo.moho.presence.impl.AbstractResource;
import com.voxeo.moho.presence.impl.StoreHolder;
import com.voxeo.moho.presence.impl.sip.SIPPresenceStore;
import com.voxeo.moho.presence.sip.SIPResource;
import com.voxeo.moho.spi.ExecutionContext;

@SuppressWarnings("serial")
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
  public SubscriptionContext getSubscriptions() {
    return null;
  }

  protected SIPPresenceStore getStore() {
    return (SIPPresenceStore) super.getStore();
  }
  
  protected void insertSubscriptionContext(SubscriptionContext context) {
    getStore().addSubscription(context);
  }
  
  protected void removeSubscriptionContext(SubscriptionContext context) {
    getStore().removeSubscription(context);
  }
  
//  @Override
//  public SubscriptionState addSubscription(SubscriptionContext context) {
//    PresenceStore presenceStore = StoreHolder.getPresenceStore();
//    presenceStore.insertSubscription(context);
//    return SubscriptionState.
//  }

//  @Override
//  public SubscriptionState updateSubscripton(SubscriptionContext context) {
//    // TODO Auto-generated method stub
//
//  }
//
//  @Override
//  public SubscriptionState removeSubscripton(SubscriptionContext context) {
//    // TODO Auto-generated method stub
//
//  }
  
  @Override
  public NotifyBody getNotifyBody(String notifyBodyType) {
    SIPPresenceStore presenceStore = (SIPPresenceStore) StoreHolder.getPresenceStore();
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

package com.voxeo.moho.presence.impl;


import com.voxeo.moho.event.SubscribeEvent.SubscriptionContext;
import com.voxeo.moho.presence.PresenceStore;
import com.voxeo.moho.presence.Resource;
import com.voxeo.moho.spi.ExecutionContext;

public abstract class AbstractResource implements Resource {
  
  protected transient ExecutionContext _context;
  
  private String _uri;
  
  public AbstractResource(ExecutionContext context, String resourceUri) {
    _context = context;
    _uri = resourceUri;
  }

  @Override
  public SubscriptionContext getSubscriptions() {
    return null;
  }

  protected void insertSubscriptionContext(SubscriptionContext context) {
    PresenceStore presenceStore = StoreHolder.getPresenceStore();
    presenceStore.addSubscription(context);
  }
  
  protected void removeSubscriptionContext(SubscriptionContext context) {
    PresenceStore presenceStore = StoreHolder.getPresenceStore();
    presenceStore.removeSubscripton(context);
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
  public void setExecutionContext(ExecutionContext context) {
    _context = context;
  }
  
  @Override
  public String getUri() {
    return _uri;
  }

}

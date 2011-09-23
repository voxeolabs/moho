package com.voxeo.moho.presence;

import java.util.Map;

import com.voxeo.moho.event.SubscribeEvent.SubscriptionContext;

public interface PresenceStore {
  
  void init(Map<String, String> props);
  
  void addSubscription(SubscriptionContext context);
  
  void updateSubscripton(SubscriptionContext context);
  
  void removeSubscripton(SubscriptionContext context);
  
  boolean isSubscriptionExist(SubscriptionContext context);
  
  Resource getResource(String resourceUri, String eventName);
  
  void addResource(Resource resource);
  
  void updateResource(Resource resource);
  
  NotifyBody getNotifyBody(String resourceUri, String eventName, String notifyBodyType);
  
  void startTx();
  
  void commitTx();
  
  void rollbackTx();
  
  void destroy();
  
  <T> void addRetrieveListener(Class<?> clazz, StoreRetrieveListener<T> listener);
  
  <T> void removeRetrieveListener(Class<?> clazz);
}

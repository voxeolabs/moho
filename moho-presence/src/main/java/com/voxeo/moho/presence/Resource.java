package com.voxeo.moho.presence;

import java.io.Serializable;

import com.voxeo.moho.event.SubscribeEvent.SubscriptionContext;
import com.voxeo.moho.spi.ExecutionContext;

public interface Resource extends Serializable {
  
  SubscriptionContext getSubscriptions();
  
  NotifyBody getNotifyBody(String notifyBodyType);
  
  SubscriptionState addSubscription(SubscriptionContext context);
  
  SubscriptionState updateSubscripton(SubscriptionContext context);
  
  SubscriptionState removeSubscripton(SubscriptionContext context);
  
  String getUri();
  
  void setExecutionContext(ExecutionContext context);
}

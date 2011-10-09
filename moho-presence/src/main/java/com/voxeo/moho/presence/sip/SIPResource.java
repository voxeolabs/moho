package com.voxeo.moho.presence.sip;

import com.voxeo.moho.event.SubscribeEvent.SubscriptionContext;
import com.voxeo.moho.presence.NotifyBody;
import com.voxeo.moho.presence.Resource;
import com.voxeo.moho.presence.SubscriptionState;

public interface SIPResource extends Resource {
  String getEventName();

  SubscriptionContext getSubscriptions();

  NotifyBody getNotifyBody(String notifyBodyType);

  SubscriptionState addSubscription(SubscriptionContext context);

  SubscriptionState updateSubscripton(SubscriptionContext context);

  SubscriptionState removeSubscripton(SubscriptionContext context);
}

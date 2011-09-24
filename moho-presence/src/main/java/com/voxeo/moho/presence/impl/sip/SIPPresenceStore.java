package com.voxeo.moho.presence.impl.sip;

import java.util.List;

import com.voxeo.moho.event.SubscribeEvent.SubscriptionContext;
import com.voxeo.moho.presence.NotifyBody;
import com.voxeo.moho.presence.PresenceStore;
import com.voxeo.moho.presence.SubscriptionID;
import com.voxeo.moho.presence.sip.EventSoftState;

public interface SIPPresenceStore extends PresenceStore {

  EventSoftState getEventSoftState(String resourceUri, String entityTag);

  boolean removeEventSoftState(EventSoftState state);

  void addEventSoftState(EventSoftState state);
  
  void addNotifyBody(String resourceUri, String eventName, String notifyBodyType, NotifyBody notifyBody);
  
  List<SubscriptionID> getSubscriptions(String resourceUri, String event, String notifyBodyType);
  
  SubscriptionContext getSubscription(SubscriptionID subId);

}

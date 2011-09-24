package com.voxeo.moho.presence.sip;

import com.voxeo.moho.presence.NotifyDispatcher;
import com.voxeo.moho.services.Service;
import com.voxeo.moho.sip.SIPPublishEvent;
import com.voxeo.moho.sip.SIPSubscribeEvent;

public interface SIPPresenceService extends Service, Runnable {
  
  final String STORE_IMPL = "com.voxeo.moho.presence.store.impl";

  final String MAX_EXPIRE = "com.voxeo.moho.presence.expire.max";
  
  final String MIN_EXPIRE = "com.voxeo.moho.presence.expire.min";

  final String DOMAINS = "com.voxeo.moho.presence.domains";
  
  void doPublish(SIPPublishEvent event);
  
  void doSubscribe(SIPSubscribeEvent event);
   
  NotifyDispatcher getNotifyDispatcher();
}

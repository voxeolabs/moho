package com.voxeo.moho.presence;

import com.voxeo.moho.event.PublishEvent;
import com.voxeo.moho.event.SubscribeEvent;
import com.voxeo.moho.services.Service;

public interface PresenceService extends Service {
  
  void doSubscribe(SubscribeEvent event);
  
  void doPublish(PublishEvent event);
}

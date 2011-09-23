package com.voxeo.moho.sample;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.State;
import com.voxeo.moho.event.PublishEvent;
import com.voxeo.moho.event.SubscribeEvent;
import com.voxeo.moho.presence.PresenceService;

public class PresenceNotify implements Application {
  
  private ApplicationContext _ctx;
  
  PresenceService _service;

  @Override
  public void init(ApplicationContext ctx) {
    _ctx = ctx;
    _service = _ctx.getService(PresenceService.class);
  }

  @Override
  public void destroy() {

  }

  @State
  public void handleSubscribe(SubscribeEvent event) {
    _service.doSubscribe(event);
  }
  
  @State
  public void handlePublish(PublishEvent event) {
    _service.doPublish(event);
  }
}

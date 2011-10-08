package com.voxeo.moho.presence.impl;

import java.util.Map;

import com.voxeo.moho.event.AcceptableEvent.Reason;
import com.voxeo.moho.event.PublishEvent;
import com.voxeo.moho.event.SubscribeEvent;
import com.voxeo.moho.presence.PresenceService;
import com.voxeo.moho.presence.sip.SIPPresenceService;
import com.voxeo.moho.sip.SIPPublishEvent;
import com.voxeo.moho.sip.SIPSubscribeEvent;
import com.voxeo.moho.spi.ExecutionContext;

public class PresenceServiceImpl implements PresenceService {

  private ExecutionContext _context;

  private SIPPresenceService _sipPesenceService;

  @Override
  public void doSubscribe(SubscribeEvent event) {
    if (event instanceof SIPSubscribeEvent) {
      if (_sipPesenceService == null) {
        _sipPesenceService = _context.getService(SIPPresenceService.class);
      }
      _sipPesenceService.doSubscribe((SIPSubscribeEvent) event);
    }
    else {
      event.reject(Reason.DECLINE);
    }
  }

  @Override
  public void doPublish(PublishEvent event) {
    if (event instanceof SIPPublishEvent) {
      if (_sipPesenceService == null) {
        _sipPesenceService = _context.getService(SIPPresenceService.class);
      }
      _sipPesenceService.doPublish((SIPPublishEvent) event);
    }
    else {
      event.reject(Reason.DECLINE);
    }
  }

  @Override
  public void init(ExecutionContext context, Map<String, String> props) throws Exception {
    _context = context;
  }

  @Override
  public void destroy() {
    if (_sipPesenceService != null) {
      _sipPesenceService.destroy();
      _sipPesenceService = null;
    }
  }

  @Override
  public String getName() {
    return PresenceService.class.getName();
  }
}

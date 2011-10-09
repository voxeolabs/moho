package com.voxeo.moho.sample;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.State;
import com.voxeo.moho.presence.xmpp.XMPPPresenceService;
import com.voxeo.moho.xmpp.XMPPIQEvent;
import com.voxeo.moho.xmpp.XMPPMessageEvent;
import com.voxeo.moho.xmpp.XMPPPresenceEvent;

public class XmppPresence implements Application {
  
  private ApplicationContext _ctx;

  @Override
  public void init(ApplicationContext ctx) {
    _ctx = ctx;
  }

  @Override
  public void destroy() {

  }

  @State
  public void handleRosterEvent(XMPPIQEvent event) {
    _ctx.getService(XMPPPresenceService.class).doIQ(event);
  }
  
  @State
  public void handlePresenceEvent(XMPPPresenceEvent event) {
    _ctx.getService(XMPPPresenceService.class).doPresence(event);
  }
  
  @State
  public void handleMessageEvent(XMPPMessageEvent event) {
    _ctx.getService(XMPPPresenceService.class).doMessage(event);
  }
}

package com.voxeo.moho.presence.xmpp.impl;


public class XmppPresenceServiceRegistrar {
  private static XMPPPresenceServiceImpl SERVICE = null;

  public static final void registerService(final XMPPPresenceServiceImpl service) {
    SERVICE = service;
  }

  public static XMPPPresenceServiceImpl findService() {
    return SERVICE;
  }
}

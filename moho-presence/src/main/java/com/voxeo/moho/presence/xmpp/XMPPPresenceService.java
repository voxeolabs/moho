package com.voxeo.moho.presence.xmpp;

import java.io.IOException;

import javax.servlet.ServletException;

import com.voxeo.moho.services.Service;
import com.voxeo.moho.xmpp.XMPPIQEvent;
import com.voxeo.moho.xmpp.XMPPMessageEvent;
import com.voxeo.moho.xmpp.XMPPPresenceEvent;
import com.voxeo.servlet.xmpp.JID;
import com.voxeo.servlet.xmpp.XmppFactory;
import com.voxeo.servlet.xmpp.XmppSessionsUtil;

public interface XMPPPresenceService extends Service {
  final String STORE_IMPL = "com.voxeo.moho.presence.xmpp.store.impl";
  
  void doPresence(XMPPPresenceEvent event);

  void doIQ(XMPPIQEvent event);
  
  void doMessage(XMPPMessageEvent event);
  
  void sendPresenceProbe(JID from, JID to) throws ServletException, IOException;
  
  boolean isResourceInterested(JID jid);
  
  boolean isResourceAvailable(JID jid);
  
  boolean isSubscriptionExists(JID from, JID to);
  
  XmppSessionsUtil getSessionUtil();
  
  XmppFactory getXmppFactory();
  
  RosterService getRosterService();
}

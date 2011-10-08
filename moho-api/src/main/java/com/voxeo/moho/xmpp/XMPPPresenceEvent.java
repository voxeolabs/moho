package com.voxeo.moho.xmpp;

import com.voxeo.moho.Framework;
import com.voxeo.servlet.xmpp.XmppServletMessage;

public interface XMPPPresenceEvent extends XMPPEvent<Framework> {
  final String TYPE_UNAVAILABLE = XmppServletMessage.TYPE_UNAVAILABLE;

  final String TYPE_SUBSCRIBE = XmppServletMessage.TYPE_SUBSCRIBE;

  final String TYPE_UNSUBSCRIBE = XmppServletMessage.TYPE_UNSUBSCRIBE;

  final String TYPE_SUBSCRIBED = XmppServletMessage.TYPE_SUBSCRIBED;

  final String TYPE_UNSUBSCRIBED = XmppServletMessage.TYPE_UNSUBSCRIBED;

  final String TYPE_PROBE = XmppServletMessage.TYPE_PROBE;

  String getStatus();
  
  boolean isAvailable();
}

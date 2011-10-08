package com.voxeo.moho.util;

import com.voxeo.moho.event.EventSource;
import com.voxeo.servlet.xmpp.XmppServletMessage;
import com.voxeo.servlet.xmpp.XmppSession;

public class XMPPSessionUtils {
  private static final String SESSION_EVENTSOURCE = "session.event.source";

  public static EventSource getEventSource(final XmppSession session) {
    return (EventSource) session.getAttribute(SESSION_EVENTSOURCE);
  }

  public static EventSource getEventSource(final XmppServletMessage message) {
    return getEventSource(message.getSession());
  }

  public static void setEventSource(final XmppSession session, final EventSource source) {
    session.setAttribute(SESSION_EVENTSOURCE, source);
  }

  public static void removeEventSource(final XmppSession session) {
    session.removeAttribute(SESSION_EVENTSOURCE);
  }
}

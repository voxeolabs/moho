package com.voxeo.moho.xmpp;

import com.voxeo.moho.event.Event;
import com.voxeo.moho.event.EventSource;
import com.voxeo.servlet.xmpp.XmppServletRequest;

public interface XMPPEvent<T extends EventSource> extends Event<T> {
  XmppServletRequest getXmppRequest();
}

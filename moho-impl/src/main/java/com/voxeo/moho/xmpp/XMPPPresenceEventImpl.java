package com.voxeo.moho.xmpp;

import com.voxeo.moho.Framework;
import com.voxeo.servlet.xmpp.PresenceMessage;
import com.voxeo.servlet.xmpp.XmppServletRequest;

public class XMPPPresenceEventImpl extends XMPPEventImpl implements XMPPPresenceEvent {
  
  public XMPPPresenceEventImpl(Framework framework, XmppServletRequest request) {
    super(framework, request);
  }

  @Override
  public boolean isProcessed() {
    return true;
  }
  
  public String getStatus() {
    return ((PresenceMessage)_request).getStatus();
  }
  
  public boolean isAvailable() {
    return ((PresenceMessage)_request).isAvailable();
  }
}

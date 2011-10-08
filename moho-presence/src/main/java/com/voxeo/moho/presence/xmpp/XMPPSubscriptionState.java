package com.voxeo.moho.presence.xmpp;

import com.voxeo.moho.presence.SubscriptionState;

public interface XMPPSubscriptionState extends SubscriptionState {
  public static enum State {
    NONE, TO, FROM, BOTH, PENDING_OUT, PENDING_IN
  }
  
  State getInboundStatus();
  
  State getOutboundStatus();
}

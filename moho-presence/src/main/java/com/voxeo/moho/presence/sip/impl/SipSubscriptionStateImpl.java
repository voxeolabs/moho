package com.voxeo.moho.presence.sip.impl;

import com.voxeo.moho.presence.sip.SipSubscriptionState;

public class SipSubscriptionStateImpl implements SipSubscriptionState {

  private static final long serialVersionUID = 8105981366331981846L;

  public static final SipSubscriptionState ALLOW = new SipSubscriptionStateImpl(200, "approved", "active");

  public static final SipSubscriptionState CONFIRM = new SipSubscriptionStateImpl(202, "subscribe", "pending");

  public static final SipSubscriptionState BLOCK = new SipSubscriptionStateImpl(403, "rejected", "terminated");

  public static final SipSubscriptionState POLITE_BLOCK = new SipSubscriptionStateImpl(202, "subscribe", "active");

  public static final SipSubscriptionState TERMINATED = new SipSubscriptionStateImpl(-1, "timeout", "terminated");

  private final int _responseCode;

  private final String _reason;

  private final String _phrase;

  public SipSubscriptionStateImpl(int responseCode, String reason, String phrase) {
    _responseCode = responseCode;
    _reason = reason;
    _phrase = phrase;
  }

  public String getReason() {
    return _reason;
  }

  public String getPhrase() {
    return _phrase;
  }

  public int getResponseCode() {
    return _responseCode;
  }

  @Override
  public String toString() {
    return "SipSubscriptionState [_reason=" + _reason + ", _phrase=" + _phrase + "]";
  }
}

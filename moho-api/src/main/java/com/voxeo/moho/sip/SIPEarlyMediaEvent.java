package com.voxeo.moho.sip;

import javax.servlet.sip.SipServletResponse;

import com.voxeo.moho.event.EarlyMediaEvent;
import com.voxeo.moho.event.EventSource;

public abstract class SIPEarlyMediaEvent extends EarlyMediaEvent {

  protected SipServletResponse _res;

  protected SIPEarlyMediaEvent(final EventSource source, final SipServletResponse res) {
    super(source);
    _res = res;
  }

  public SipServletResponse getSipResponse() {
    return _res;
  }
}

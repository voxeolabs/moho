package com.voxeo.moho.sip;

import javax.servlet.sip.SipServletRequest;

import com.voxeo.moho.ExecutionContext;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.TextEvent;

public abstract class SIPTextEvent extends TextEvent {

  protected SipServletRequest _req;

  protected ExecutionContext _ctx;

  protected SIPTextEvent(final EventSource source, final SipServletRequest req) {
    super(source);
    _req = req;
    _ctx = (ExecutionContext) source.getApplicationContext();
  }

  public SipServletRequest getSipRequest() {
    return _req;
  }

}

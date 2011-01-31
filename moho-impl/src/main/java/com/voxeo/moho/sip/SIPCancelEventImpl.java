package com.voxeo.moho.sip;

import java.util.Map;

import javax.servlet.sip.SipServletRequest;

import com.voxeo.moho.SignalException;
import com.voxeo.moho.event.EventSource;

public class SIPCancelEventImpl extends SIPCancelEvent {

  protected SIPCancelEventImpl(final EventSource source, final SipServletRequest req) {
    super(source, req);
  }

  @Override
  public synchronized void accept(final Map<String, String> headers) throws SignalException {
    this.checkState();
    _accepted = true;
    if (this.source instanceof SIPIncomingCall) {
      final SIPIncomingCall retval = (SIPIncomingCall) this.source;
      retval.doCancel();
    }
  }

}

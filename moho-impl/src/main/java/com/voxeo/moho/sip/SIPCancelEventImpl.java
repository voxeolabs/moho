package com.voxeo.moho.sip;

import java.util.Map;

import javax.servlet.sip.SipServletRequest;

import com.voxeo.moho.SignalException;
import com.voxeo.moho.event.EventSource;

public class SIPCancelEventImpl extends SIPCancelEvent {

  protected SIPCancelEventImpl(EventSource source, SipServletRequest req) {
    super(source, req);
  }

  @Override
  public synchronized void accept(final Map<String, String> headers) throws SignalException {
    this.checkState();
    this.setState(AcceptableEventState.ACCEPTED);
    if (this.source instanceof SIPIncomingCall) {
      final SIPIncomingCall retval = (SIPIncomingCall) this.source;
      retval.doCancel();
    }
  }

}

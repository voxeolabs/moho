package com.voxeo.moho.sip;

import java.util.Map;

import javax.servlet.sip.SipServletResponse;

import org.apache.log4j.Logger;

import com.voxeo.moho.Constants;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.common.event.MohoEarlyMediaEvent;

public class SIPEarlyMediaEventImpl extends MohoEarlyMediaEvent implements SIPEarlyMediaEvent {
  private static final Logger LOG = Logger.getLogger(SIPEarlyMediaEventImpl.class);

  protected SipServletResponse _res;

  protected SIPEarlyMediaEventImpl(final SIPCall source, final SipServletResponse res) {
    super(source);
    _res = res;
  }

  @Override
  public SipServletResponse getSipResponse() {
    return _res;
  }

  @Override
  public void reject(final Reason reason) throws SignalException {
    reject(reason, null);
  }

  @Override
  public void reject(final Reason reason, final Map<String, String> headers) throws SignalException {
    this.checkState();
    _rejected = true;
    if (source instanceof SIPCallImpl) {
      final SIPCallImpl call = (SIPCallImpl) source;

      try {
        call.doResponse(_res, headers);
      }
      catch (final Exception e) {
        throw new SignalException(e);
      }
    }
    // do the following in delegate
    // if join to media server, process as normal.

    // if bridge, don't join the two network at this point

    // if direct, don't send the SDP this to the peer.
  }

  @Override
  public void accept(final Map<String, String> headers) throws SignalException, IllegalStateException {
    this.checkState();
    _accepted = true;

    if (source instanceof SIPCallImpl) {
      final SIPCallImpl call = (SIPCallImpl) source;
      _res.setAttribute(Constants.Attribute_AcceptEarlyMedia, "true");
      try {
        call.doResponse(_res, headers);
      }
      catch (final Exception e) {
        throw new SignalException(e);
      }
      
      if(call.getJoinDelegate() != null && call.getJoinDelegate() instanceof Media2NOJoinDelegate){
        while (!call.isTerminated() && (call.getSIPCallState() == SIPCall.State.PROGRESSING)) {
          try {
            synchronized (call) {
              call.wait(500);
            }
          }
          catch (final InterruptedException e) {
            // ignore
          }
        }
        LOG.debug(call + " EarlyMediaEvent accepted.");
      }
      // do the following in delegate
      // if join to media server, process as normal.

      // if bridge, join networks of two call.
      
      // if direct, send the SDP this to the peer.
    }
  }
}

package com.voxeo.moho.sip;

import java.util.Map;

import javax.servlet.sip.SipServletResponse;

import com.voxeo.moho.Call;
import com.voxeo.moho.Constants;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.common.event.MohoErrorRingEvent;

public class SIPErrorRingEventImpl extends MohoErrorRingEvent implements SIPErrorRingEvent {

  protected SipServletResponse _res;

  public SIPErrorRingEventImpl(Call source, SipServletResponse _res) {
    super(source);
    this._res = _res;
  }

  @Override
  public void accept(Map<String, String> headers) throws SignalException {
    this.checkState();
    _accepted = true;

    if (source instanceof SIPCallImpl) {
      final SIPCallImpl call = (SIPCallImpl) source;
      _res.setAttribute(Constants.Attribute_BridgeEarlyMedia, "true");
      try {
        call.doResponse(_res, headers);
      }
      catch (final Exception e) {
        throw new SignalException(e);
      }
    }
  }

  @Override
  public void reject(Reason reason, Map<String, String> headers) throws SignalException {
    checkState();
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
  }

  @Override
  public SipServletResponse getSipResponse() {
    return _res;
  }

}

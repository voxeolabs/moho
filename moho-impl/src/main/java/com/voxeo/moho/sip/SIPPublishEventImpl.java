package com.voxeo.moho.sip;

import java.io.IOException;
import java.util.Map;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import com.voxeo.moho.Framework;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.common.event.MohoPublishEvent;
import com.voxeo.moho.spi.ExecutionContext;
import com.voxeo.moho.spi.SpiFramework;

public class SIPPublishEventImpl extends MohoPublishEvent implements SIPPublishEvent {

  protected SipServletRequest _req;
  
  protected ExecutionContext _ctx;

  protected SIPPublishEventImpl(final Framework source, final SipServletRequest req) {
    super(source);
    _ctx = ((SpiFramework) source).getExecutionContext();
    _req = req;
  }

  public SipServletRequest getSipRequest() {
    return _req;
  }
  
  @Override
  public synchronized void accept(final Map<String, String> headers) throws SignalException, IllegalStateException {
    this.checkState();
    _accepted = true;
    final SipServletResponse res = _req.createResponse(SipServletResponse.SC_OK);
    SIPHelper.addHeaders(res, headers);
    try {
      res.send();
    }
    catch (final IOException e) {
      throw new SignalException(e);
    }
  }

  @Override
  public synchronized void reject(Reason reason, Map<String, String> headers) throws SignalException {
    this.checkState();
    _rejected = true;
    final SipServletResponse res = _req.createResponse(reason == null ? Reason.DECLINE.getCode() : reason.getCode());
    SIPHelper.addHeaders(res, headers);
    try {
      res.send();
    }
    catch (final IOException e) {
      throw new SignalException(e);
    }
  }
}

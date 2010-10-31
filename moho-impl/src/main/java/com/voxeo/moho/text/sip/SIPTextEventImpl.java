package com.voxeo.moho.text.sip;

import java.io.IOException;
import java.util.Map;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import com.voxeo.moho.SignalException;
import com.voxeo.moho.TextableEndpoint;
import com.voxeo.moho.event.ApplicationEventSource;
import com.voxeo.moho.sip.SIPEndpointImpl;
import com.voxeo.moho.sip.SIPHelper;
import com.voxeo.moho.sip.SIPTextEvent;

public class SIPTextEventImpl extends SIPTextEvent {

  public SIPTextEventImpl(final ApplicationEventSource source, final SipServletRequest req) {
    super(source, req);
  }

  @Override
  public String getText() {
    Object content = null;
    try {
      content = _req.getContent();
    }
    catch (final IOException e) {
      throw new SignalException(e);
    }
    return content == null ? null : String.valueOf(content);
  }

  @Override
  public String getTextType() {
    return _req.getContentType();
  }

  @Override
  public TextableEndpoint getSource() {
    return new SIPEndpointImpl(_ctx, _req.getFrom());
  }

  @Override
  public TextableEndpoint getDestination() {
    return new SIPEndpointImpl(_ctx, _req.getTo());
  }

  @Override
  public void accept() throws SignalException, IllegalStateException {
    final SipServletResponse res = _req.createResponse(SipServletResponse.SC_OK);
    try {
      res.send();
    }
    catch (final IOException ex) {
      throw new SignalException(ex);
    }
  }

  @Override
  public void accept(final Map<String, String> headers) throws SignalException, IllegalStateException {
    final SipServletResponse res = _req.createResponse(SipServletResponse.SC_OK);
    SIPHelper.addHeaders(res, headers);
    try {
      res.send();
    }
    catch (final IOException ex) {
      throw new SignalException(ex);
    }
  }

}

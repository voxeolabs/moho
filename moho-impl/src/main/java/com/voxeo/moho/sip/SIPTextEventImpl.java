package com.voxeo.moho.sip;

import java.io.IOException;
import java.util.Map;

import javax.servlet.sip.SipServletRequest;

import com.voxeo.moho.Endpoint;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.TextableEndpoint;
import com.voxeo.moho.common.event.MohoTextEvent;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.sip.SIPTextEvent;
import com.voxeo.moho.spi.ExecutionContext;

public class SIPTextEventImpl<T extends EventSource> extends MohoTextEvent<T> implements SIPTextEvent<T> {

  protected SipServletRequest _req;

  protected ExecutionContext _ctx;
  
  protected boolean _proxied;

  protected SIPTextEventImpl(final T source, final SipServletRequest req) {
    super(source);
    _req = req;
    _ctx = (ExecutionContext) source.getApplicationContext();
  }

  @Override
  public SipServletRequest getSipRequest() {
    return _req;
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
  public TextableEndpoint getFrom() {
    return new SIPEndpointImpl(_ctx, _req.getFrom());
  }

  @Override
  public TextableEndpoint getTo() {
    return new SIPEndpointImpl(_ctx, _req.getTo());
  }

  @Override
  public boolean isProxied() {
    return _proxied;
  }
  
  protected synchronized boolean isProcessed() {
    return isProxied();
  }
  
  protected synchronized void checkState() {
    if (isProcessed()) {
      throw new IllegalStateException("Event is already processed and can not be processed.");
    }
  }

  @Override
  public void proxyTo(boolean recordRoute, boolean parallel, Endpoint... destinations) throws SignalException {
    proxyTo(recordRoute, parallel, null, destinations);
  }

  @Override
  public synchronized void proxyTo(boolean recordRoute, boolean parallel, Map<String, String> headers, Endpoint... destinations) {
    checkState();
    _proxied = true;
    SIPHelper.proxyTo(_ctx.getSipFactory(), _req, headers, recordRoute, parallel, destinations);
  }

}

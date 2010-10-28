package com.voxeo.moho.text.sip;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import com.voxeo.moho.SignalException;
import com.voxeo.moho.TextableEndpoint;
import com.voxeo.moho.event.ApplicationEventSource;
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
      e.printStackTrace();
    }
    return content == null ? null : String.valueOf(content);
  }

  @Override
  public String getTextType() {
    return _req.getContentType();
  }

  @Override
  public TextableEndpoint getSource() {
    return (TextableEndpoint) _ctx.getEndpoint(_req.getFrom().getURI().toString());
  }

  @Override
  public TextableEndpoint getDestination() {
    return (TextableEndpoint) _ctx.getEndpoint(_req.getTo().getURI().toString());
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

  private static final Pattern P = Pattern.compile("<[^>]+>([^<]+)<[^>]+>");

  // get text content in a sip request.
  private String getContent(final SipServletRequest sipReq) throws UnsupportedEncodingException, IOException {
    String s = sipReq.getContent().toString();
    if (sipReq.getContentType().equalsIgnoreCase("text/html")) {
      final Matcher m = P.matcher(s);
      String a = "";
      while (m.find()) {
        a += m.group(1);
      }
      s = a;
    }

    s = s.trim();
    if (s.endsWith("\r\n")) {
      s = s.substring(0, s.length() - 2);
      s = s.trim();
    }
    return s;
  }

}

package com.voxeo.moho.xmpp;

import java.util.List;

import org.w3c.dom.Element;

import com.voxeo.moho.Framework;
import com.voxeo.moho.TextableEndpoint;
import com.voxeo.moho.event.MohoTextEvent;
import com.voxeo.moho.spi.ExecutionContext;
import com.voxeo.servlet.xmpp.JID;
import com.voxeo.servlet.xmpp.XmppServletRequest;

public class XMPPMessageEventImpl extends MohoTextEvent<Framework> implements XMPPMessageEvent {
  
  private XmppServletRequest _request;
  
  protected ExecutionContext _ctx;
  
  public XMPPMessageEventImpl(Framework framework, XmppServletRequest request) {
    super(framework);
    _request = request;
    _ctx = (ExecutionContext) source.getApplicationContext();
  }

  @Override
  public XmppServletRequest getXmppRequest() {
    return _request;
  }

  @Override
  public String getText() {
    return _request.getElement("body").getTextContent();
  }

  @Override
  public String getTextType() {
    return _request.getContentType();
  }

  @Override
  public TextableEndpoint getFrom() {
    return new XMPPEndpointImpl(_ctx, _request.getFrom());
  }

  @Override
  public TextableEndpoint getTo() {
    return new XMPPEndpointImpl(_ctx, _request.getTo());
  }

  @Override
  public boolean isProcessed() {
    return true;
  }

  @Override
  public String getType() {
    return _request.getType();
  }

  @Override
  public JID getMessageFrom() {
    return _request.getFrom();
  }

  @Override
  public JID getMessageTo() {
    return _request.getTo();
  }

  @Override
  public List<Element> getContent() {
    return _request.getElements();
  }
  
  @Override
  public Element getElement() {
    return _request.getElement();
  }
  
  @Override
  public Element getElement(String name) {
    return _request.getElement(name);
  }
}

package com.voxeo.moho.xmpp;

import java.util.List;

import org.w3c.dom.Element;

import com.voxeo.moho.Framework;
import com.voxeo.moho.common.event.MohoEvent;
import com.voxeo.servlet.xmpp.JID;
import com.voxeo.servlet.xmpp.XmppServletRequest;

public abstract class XMPPEventImpl extends MohoEvent<Framework> implements XMPPEvent<Framework> {
  
  protected XmppServletRequest _request;
  
  protected boolean _processed;

  public XMPPEventImpl(Framework source, XmppServletRequest request) {
    super(source);
    _request = request;
  }

  @Override
  public XmppServletRequest getXmppRequest() {
    return _request;
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
  public String getType() {
    return _request.getType();
  }
  
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
  
  @Override
  public String toString() {
    return "XMPPEvent[" + _request  + "]";
  }
}

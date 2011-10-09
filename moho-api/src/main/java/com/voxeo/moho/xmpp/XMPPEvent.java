package com.voxeo.moho.xmpp;

import java.util.List;

import org.w3c.dom.Element;

import com.voxeo.moho.event.Event;
import com.voxeo.moho.event.EventSource;
import com.voxeo.servlet.xmpp.JID;
import com.voxeo.servlet.xmpp.XmppServletRequest;

public interface XMPPEvent<T extends EventSource> extends Event<T> {
  XmppServletRequest getXmppRequest();
  
  String getType();
  
  JID getMessageFrom();
  
  JID getMessageTo();
  
  List<Element> getContent();
  
  Element getElement();
  
  Element getElement(String name);
  
  boolean isProcessed();
}

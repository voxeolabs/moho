package com.voxeo.moho.presence.xmpp;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import javax.servlet.ServletException;

import org.w3c.dom.Element;

import com.voxeo.moho.spi.ExecutionContext;

public interface XmppPendingNotification extends Serializable, Cloneable {
  
  void triggerSendToSubscribee() throws IOException, ServletException;

  Element[] getPresenceStanza();

  void setType(String type);
  
  String getType();
  
  String getFrom();
  
  String getTo();

  void setPresenceStanza(List<Element> stanza);
  
  void setExecutionContext(ExecutionContext context);
  
  XmppPendingNotification clone();
}

package com.voxeo.moho.presence.xmpp.impl;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;

import org.w3c.dom.Element;

import com.voxeo.moho.presence.impl.StoreHolder;
import com.voxeo.moho.presence.impl.xmpp.XMPPPresenceStore;
import com.voxeo.moho.presence.xmpp.XmppPendingNotification;
import com.voxeo.moho.spi.ExecutionContext;
import com.voxeo.moho.xmpp.XMPPPresenceEvent;
import com.voxeo.servlet.xmpp.XmppFactory;

public class XmppPendingNotificationImpl implements XmppPendingNotification {

  private static final long serialVersionUID = -8143793543850243825L;

  private final String _from;
  
  private final String _to;

  private Element[] _presenceStanza;

  private String _type;

  private transient ExecutionContext _ctx;

  private transient XmppFactory _xmppFactory;

  public XmppPendingNotificationImpl(ExecutionContext ctx, String from, String to) {
    _ctx = ctx;
    _from = from;
    _to = to;
    _xmppFactory = _ctx.getXmppFactory();
  }

  public String getFrom() {
    return _from;
  }

  public String getTo() {
    return _to;
  }

  public void setPresenceStanza(List<Element> stanza) {
    _presenceStanza = stanza == null ? null : stanza.toArray(new Element[stanza.size()]);
  }

  @Override
  public void setExecutionContext(ExecutionContext context) {
    _ctx = context;
  }

  public void triggerSendToSubscribee() throws IOException, ServletException {
    getXmppFactory().createPresence(_from, _to, _type, getPresenceStanza()).send();
    if (!getType().equals(XMPPPresenceEvent.TYPE_SUBSCRIBE)) {
      ((XMPPPresenceStore)StoreHolder.getPresenceStore()).removeNotification(this);
    }
  }

  public String getType() {
    return _type;
  }

  public void setType(String type) {
    _type = type;
  }

  public Element[] getPresenceStanza() {
    return _presenceStanza;
  }

  public XmppFactory getXmppFactory() {
    if (_xmppFactory == null) {
      _xmppFactory = _ctx.getXmppFactory();
    }
    return _xmppFactory;
  }

  public XmppPendingNotificationImpl clone() {
    try {
      return (XmppPendingNotificationImpl) super.clone();
    }
    catch (CloneNotSupportedException e) {
      ;
    }
    return null;
  }
}

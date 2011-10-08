package com.voxeo.moho.presence.xmpp.impl;

import java.util.Collection;

import org.apache.log4j.Logger;

import com.voxeo.moho.presence.impl.AbstractResource;
import com.voxeo.moho.presence.impl.xmpp.XMPPPresenceStore;
import com.voxeo.moho.presence.xmpp.XMPPPresenceResource;
import com.voxeo.moho.presence.xmpp.XmppPendingNotification;
import com.voxeo.moho.spi.ExecutionContext;
import com.voxeo.servlet.xmpp.JID;

public class XMPPPresenceResourceImpl extends AbstractResource implements XMPPPresenceResource {

  private static final long serialVersionUID = -5908903825072997723L;
  
  private static final Logger LOG = Logger.getLogger(XMPPPresenceResourceImpl.class);

  private boolean _interested;

  private boolean _available;
  
  private String _bareJID;

  public XMPPPresenceResourceImpl(ExecutionContext context, JID fullJID) {
    super(context, fullJID.toString());
    _bareJID = fullJID.getBareJID().toString();
    save();
  }

  @Override
  public boolean isAvailable() {
    return _available;
  }

  @Override
  public boolean isInterested() {
    return _interested;
  }

  public void setInterested(boolean interested) {
    _interested = interested;
    save();
  }

  public void setAvailable(boolean available) {
    _available = available;
    save();
    Collection<XmppPendingNotification> subscriptions = getStore().getNotifyByTo(_context.getXmppFactory().createJID(getUri()).getBareJID().toString());
    for (XmppPendingNotification subscription : subscriptions) {
      try {
        subscription.triggerSendToSubscribee();
      }
      catch (Exception e) {
        LOG.error("Error sending pending subsciption for " + this, e);
      }
    }
  }

  protected XMPPPresenceStore getStore() {
    return (XMPPPresenceStore) super.getStore();
  }

  private void save() {
    getStore().addResource(this);
  }

  @Override
  public String toString() {
    return "XMPPPresenceResource [JID=" + getUri() + "]";
  }

  @Override
  public String getBareJID() {
    return _bareJID;
  }
}

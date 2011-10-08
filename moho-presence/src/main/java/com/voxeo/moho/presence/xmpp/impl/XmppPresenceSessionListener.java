package com.voxeo.moho.presence.xmpp.impl;

import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import com.voxeo.moho.presence.impl.xmpp.XMPPPresenceStore;
import com.voxeo.moho.presence.xmpp.Roster;
import com.voxeo.moho.xmpp.RosterEvent.RosterItem;
import com.voxeo.moho.xmpp.XMPPPresenceEvent;
import com.voxeo.servlet.xmpp.Feature;
import com.voxeo.servlet.xmpp.JID;
import com.voxeo.servlet.xmpp.XmppSessionEvent;
import com.voxeo.servlet.xmpp.XmppSessionListener;

public class XmppPresenceSessionListener implements XmppSessionListener {
  
  private static final Logger LOG = Logger.getLogger(XmppPresenceSessionListener.class);
  
  private XMPPPresenceStore _store;

  @Override
  public void sessionCreated(XmppSessionEvent xmppsessionevent) {
    
  }

  @Override
  public void sessionDestroyed(XmppSessionEvent xmppsessionevent) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("XmppPresenceSessionListener invoked due to " + xmppsessionevent);
    }
    if (_store == null) {
      _store = XmppPresenceServiceRegistrar.findService().getStore();
    }
    JID remoteJID = xmppsessionevent.getSession().getRemoteJID();
    _store.removeResource(_store.getResource(remoteJID.toString()));
    Roster roster = _store.getRoster(remoteJID.getBareJID().toString());
    if (roster != null) {
      for (RosterItem item : roster.getItems()) {
        try {
          XmppPresenceServiceRegistrar.findService().getXmppFactory()
              .createPresence(remoteJID.toString(), item.getJID(), XMPPPresenceEvent.TYPE_UNAVAILABLE, (Element[]) null).send();
        }
        catch (Exception e) {
          LOG.error("Error sending unavailable presence to " + item.getJID() + " for " + remoteJID, e);
        }
      }
    }
  }

  @Override
  public void onFeature(XmppSessionEvent event, List<Feature> features) {
    
  }

}

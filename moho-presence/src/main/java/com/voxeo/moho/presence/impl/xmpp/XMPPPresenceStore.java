package com.voxeo.moho.presence.impl.xmpp;

import java.util.Collection;
import java.util.List;

import com.voxeo.moho.presence.PresenceStore;
import com.voxeo.moho.presence.xmpp.Roster;
import com.voxeo.moho.presence.xmpp.XMPPPresenceResource;
import com.voxeo.moho.presence.xmpp.XmppPendingNotification;

public interface XMPPPresenceStore extends PresenceStore {
  
  XMPPPresenceResource getResource(String jid);
  
  List<XMPPPresenceResource> getResourceByBareID(String jid);
  
  void removeResource(XMPPPresenceResource resource);
  
  void addResource(XMPPPresenceResource resource);
  
  Collection<XmppPendingNotification> getNotifyByTo(String jid);
  
  XmppPendingNotification getNotification(String from, String to);
  
  void addNotification(XmppPendingNotification notifcation);
  
  void removeNotification(XmppPendingNotification notifcation);
  
  Roster getRoster(String user);

  void addRoster(Roster roster);

  void removeRoster(Roster roster);
}

package com.voxeo.moho.presence.impl.xmpp.memory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.voxeo.moho.presence.impl.AbstractPresenceStore;
import com.voxeo.moho.presence.impl.xmpp.XMPPPresenceStore;
import com.voxeo.moho.presence.xmpp.Roster;
import com.voxeo.moho.presence.xmpp.XMPPPresenceResource;
import com.voxeo.moho.presence.xmpp.XmppPendingNotification;

public class XMPPMemoryPresenceStore extends AbstractPresenceStore implements XMPPPresenceStore {
  
  private Map<String, Roster> _rosters = new ConcurrentHashMap<String, Roster>();
  
  private Map<String, HashMap<String, XmppPendingNotification>> _notifications = new ConcurrentHashMap<String, HashMap<String, XmppPendingNotification>>();
  
  private Map<String, XMPPPresenceResource> _resources = new ConcurrentHashMap<String, XMPPPresenceResource>();
  
  private Map<String, ArrayList<String>> _bareResource = new ConcurrentHashMap<String, ArrayList<String>>();

  @Override
  public void init(Map<String, String> props) {
//    RosterImpl roster = new RosterImpl();
    //"convergence@voxeo.com"
    //test1@voxeo.com
////    roster.addItem(new RosterItemImpl("test2@voxeo.com", XmppSubscription.FROM));
////    roster.addItem(new RosterItemImpl("test3@voxeo.com", XmppSubscription.TO));
//    _rosters.put("test1@voxeo.com/Spark 2.6.3", roster);
//    _rosters.put("convergence@voxeo.com/Spark 2.6.3", roster);
  }

  @Override
  public void startTx() {
    // TODO Auto-generated method stub

  }

  @Override
  public void commitTx() {
    // TODO Auto-generated method stub

  }

  @Override
  public void rollbackTx() {
    // TODO Auto-generated method stub

  }

  @Override
  public void destroy() {
    // TODO Auto-generated method stub

  }

  @Override
  public Roster getRoster(String user) {
    return _rosters.get(user);
  }

  @Override
  public void addRoster(Roster roster) {
    _rosters.put(roster.getOwner(), roster);
  }

  @Override
  public void removeRoster(Roster roster) {
    _rosters.remove(roster.getOwner());
  }

  @Override
  public void addNotification(XmppPendingNotification notifcation) {
    HashMap<String, XmppPendingNotification> fromMap = _notifications.get(notifcation.getTo());
    if (fromMap == null) {
      fromMap = new HashMap<String, XmppPendingNotification>();
      _notifications.put(notifcation.getTo(), fromMap);
    }
    fromMap.put(notifcation.getFrom(), notifcation);
  }


  @Override
  public void removeNotification(XmppPendingNotification notifcation) {
    HashMap<String, XmppPendingNotification> fromMap = _notifications.get(notifcation.getTo());
    if (fromMap != null) {
      fromMap.remove(notifcation.getFrom());
    }
  }

  @Override
  public XmppPendingNotification getNotification(String from, String to) {
    HashMap<String, XmppPendingNotification> fromMap = _notifications.get(to);
    if (fromMap != null) {
      return fromMap.get(from).clone();
    }
    return null;
  }
  
  public XMPPPresenceResource getResource(String jid) {
    XMPPPresenceResource resource = _resources.get(jid);
    if (resource != null) {
      return (XMPPPresenceResource) resource.clone();
    }
    return null;
  }
  
  public List<XMPPPresenceResource> getResourceByBareID(String jid) {
    List<String> resources = _bareResource.get(jid);
    if (resources != null) {
      List<XMPPPresenceResource> list = new ArrayList<XMPPPresenceResource>(resources.size());
      for (String fullJid : resources) {
        XMPPPresenceResource resource = getResource(fullJid);
        if (resource != null) {
          list.add(resource);
        }
      }
      return Collections.unmodifiableList(list);
    }
    return Collections.emptyList();
  }

  @Override
  public void addResource(XMPPPresenceResource resource) {
    _resources.put(resource.getUri(), resource);
    addToBareResources(resource.getUri(), resource.getBareJID());
  }
  
  private void addToBareResources(String fullJID, String bareJID) {
    ArrayList<String> resources = _bareResource.get(bareJID);
    if (resources == null) {
      resources = new ArrayList<String>();
      _bareResource.put(bareJID, resources);
    }
    resources.add(fullJID);
  }

  @Override
  public Collection<XmppPendingNotification> getNotifyByTo(String jid) {
    if (_notifications.get(jid) != null) {
      return Collections.unmodifiableCollection(_notifications.get(jid).values());
    }
    else {
      return Collections.emptySet();
    }
  }

  @Override
  public void removeResource(XMPPPresenceResource resource) {
    _resources.remove(resource.getUri());
    removeBareResource(resource.getBareJID(), resource.getUri());
  }
  
  private void removeBareResource(String bareJID, String fullJID) {
    List<String> resources = _bareResource.get(bareJID);
    if (resources != null) {
      resources.remove(fullJID);
    }
  }
}

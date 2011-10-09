package com.voxeo.moho.presence.xmpp;

import java.io.Serializable;
import java.util.Collection;

import com.voxeo.moho.xmpp.RosterEvent.RosterItem;
import com.voxeo.servlet.xmpp.JID;

public interface Roster extends Serializable, Cloneable {
  String getOwner();

  Collection<RosterItem> getItems();

  RosterItem getItem(String to);
  
  RosterItem getItem(JID to);
  
  void addItem(RosterItem item);
  
  void removeItem(RosterItem item);
  
  Roster clone();
}

package com.voxeo.moho.presence.xmpp.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.voxeo.moho.presence.xmpp.Roster;
import com.voxeo.moho.xmpp.RosterEvent.RosterItem;
import com.voxeo.servlet.xmpp.JID;

public class RosterImpl implements Roster {

  private static final long serialVersionUID = 8253609834727154083L;

  private String _userName;

  private Map<String, RosterItem> _items = new HashMap<String, RosterItem>();

  public RosterImpl(String owner) {
    _userName = owner;
  }

  @Override
  public String getOwner() {
    return _userName;
  }

  @Override
  public Collection<RosterItem> getItems() {
    return _items.values();
  }

  @Override
  public RosterItem getItem(String to) {
    return _items.get(to);
  }

  @Override
  public RosterItem getItem(JID to) {
    return getItem(to.toString());
  }

  @Override
  public void addItem(RosterItem item) {
    _items.put(item.getJID().toString(), item);
  }

  @Override
  public void removeItem(RosterItem item) {
    _items.remove(item.getJID());
  }

  public Roster clone() {
    try {
      return (Roster) super.clone();
    }
    catch (CloneNotSupportedException e) {
      ;
    }
    return null;
  }

  @Override
  public String toString() {
    return "RosterImpl [_userName=" + _userName + ", _items=" + _items + "]";
  }
}

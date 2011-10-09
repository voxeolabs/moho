package com.voxeo.moho.presence.xmpp.impl;

import java.util.ArrayList;
import java.util.List;

import com.voxeo.moho.xmpp.RosterEvent.Ask;
import com.voxeo.moho.xmpp.RosterEvent.RosterItem;
import com.voxeo.moho.xmpp.RosterEvent.XmppSubscription;

public class RosterItemImpl implements RosterItem {

  private static final long serialVersionUID = 6370325091619750992L;

  private String _jid;

  private String _name;

  private Ask _ask;

  private XmppSubscription _subscription;

  private List<String> _groups = new ArrayList<String>();
  
  private boolean _approved;

  public RosterItemImpl(String jid) {
    _jid = jid;
    _ask = Ask.NONE;
    _subscription = XmppSubscription.NONE;
  }

  @Override
  public Ask getAsk() {
    return _ask;
  }

  @Override
  public String getName() {
    return _name;
  }

  @Override
  public String getJID() {
    return _jid;
  }

  @Override
  public XmppSubscription getSubscription() {
    return _subscription;
  }

  @Override
  public List<String> getGroups() {
    return _groups;
  }

  @Override
  public void setAsk(Ask ask) {
    _ask = ask;
  }

  @Override
  public void setSubscription(XmppSubscription sub) {
    _subscription = sub;
  }

  @Override
  public void addGroupName(String group) {
    _groups.add(group);
  }

  @Override
  public void setName(String name) {
    _name = name;
  }

  @Override
  public void setPreApproved(boolean approved) {
    _approved = approved;
  }

  @Override
  public boolean getPreApproved() {
    return _approved;
  }

  @Override
  public String toString() {
    return "RosterItemImpl [_jid=" + _jid + ",  _ask=" + _ask + ", _subscription=" + _subscription + ", _groups=" + _groups + "]";
  }
}

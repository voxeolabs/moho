package com.voxeo.moho.presence.xmpp.impl;

import com.voxeo.moho.xmpp.RosterEvent;
import com.voxeo.moho.xmpp.RosterItemImpl;
import com.voxeo.moho.xmpp.RosterEvent.RosterItem;

public class RosterFactory {
  public static RosterItem createRosterItem(String jid) {
    return new RosterItemImpl(jid);
  }
}

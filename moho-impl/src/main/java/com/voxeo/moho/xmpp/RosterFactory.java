package com.voxeo.moho.xmpp;

import com.voxeo.moho.presence.xmpp.impl.RosterItemImpl;
import com.voxeo.moho.xmpp.RosterEvent.RosterItem;

public class RosterFactory {
  public static RosterItem createRosterItem(String jid) {
    return new RosterItemImpl(jid);
  }
}

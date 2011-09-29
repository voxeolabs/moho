package com.voxeo.moho.remote.impl;

import com.rayo.client.xmpp.stanza.IQ;
import com.rayo.client.xmpp.stanza.Presence;

public interface RayoListener {

  public void onRayoEvent(JID from, Presence presence);

  public void onRayoCommandResult(JID from, IQ iq);

}

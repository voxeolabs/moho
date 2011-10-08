package com.voxeo.moho.xmpp;

import com.voxeo.moho.event.EventSource;

public interface RosterPush extends EventSource {
  void send();
}

package com.voxeo.moho.spi;

import com.voxeo.moho.conference.ConferenceManager;

public interface ConferenceDriver extends ProtocolDriver {
  ConferenceManager getManager();
}

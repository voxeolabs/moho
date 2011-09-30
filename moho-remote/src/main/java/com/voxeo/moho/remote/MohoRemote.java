package com.voxeo.moho.remote;

import java.net.URI;

import com.voxeo.moho.Endpoint;
import com.voxeo.moho.Participant;
import com.voxeo.moho.event.EventSource;

public interface MohoRemote extends EventSource {

  void connect(AuthenticationCallback callback, String server);

  void disconnect();

  Endpoint createEndpoint(URI uri);

  Participant getParticipant(final String id);
}

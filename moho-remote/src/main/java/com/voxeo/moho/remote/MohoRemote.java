package com.voxeo.moho.remote;

import java.net.URI;

import com.voxeo.moho.Endpoint;
import com.voxeo.moho.Participant;
import com.voxeo.moho.event.EventSource;

public interface MohoRemote extends EventSource {

  @Deprecated
  void connect(AuthenticationCallback callback, String xmppServer, String rayoServer);

  void connect(String userName, String passwd, String realm, String resource, String xmppServer, String rayoServer);

  void disconnect();

  Endpoint createEndpoint(URI uri);

  Participant getParticipant(final String id);
}

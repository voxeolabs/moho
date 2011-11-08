package com.voxeo.moho.remote;

import java.net.URI;

import com.voxeo.moho.Endpoint;
import com.voxeo.moho.Participant;
import com.voxeo.moho.event.EventSource;

public interface MohoRemote extends EventSource {

  @Deprecated
  void connect(AuthenticationCallback callback, String xmppServer, String rayoServer) throws MohoRemoteException;

  void connect(String userName, String passwd, String realm, String resource, String xmppServer, String rayoServer) throws MohoRemoteException;

  void connect(String userName, String passwd, String realm, String resource, String xmppServer, String rayoServer, int timeout) throws MohoRemoteException;

  void disconnect() throws MohoRemoteException;

  Endpoint createEndpoint(URI uri);
  
  Endpoint createEndpoint(String mixerName);

  Participant getParticipant(final String id);
}

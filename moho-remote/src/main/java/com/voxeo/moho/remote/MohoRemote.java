package com.voxeo.moho.remote;

import java.net.URI;

import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.event.Observer;

public interface MohoRemote {

  void connect(AuthenticationCallback callback, String server);

  void addObserver(Observer observer);

  void disconnect();

  CallableEndpoint createEndpoint(URI uri);

  Call getCall(final String cid);
}

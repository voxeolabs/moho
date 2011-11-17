package com.voxeo.moho.remote.impl;

import java.net.URI;
import java.util.Map;

import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.Subscription;
import com.voxeo.moho.Subscription.Type;

public class CallableEndpointImpl implements CallableEndpoint {
  protected URI _uri;

  protected MohoRemoteImpl _mohoRemote;

  public CallableEndpointImpl(MohoRemoteImpl mohoRemote, URI uri) {
    super();
    this._uri = uri;
    this._mohoRemote = mohoRemote;
  }

  @Override
  public String getName() {
    return _uri.toString();
  }

  @Override
  public URI getURI() {
    return _uri;
  }

  @Override
  public Call call(Endpoint caller) {
    return createCall(caller);
  }

  @Override
  public Call call(Endpoint caller, Map<String, String> headers) {
    return createCall(caller, headers);
  }

  @Override
  public Call call(String caller) {
    return createCall(caller);
  }

  @Override
  public Call call(String caller, Map<String, String> headers) {
    return createCall(caller, headers);
  }

  @Override
  public Subscription subscribe(Endpoint subscriber, Type type, int expiration) {
    throw new UnsupportedOperationException(Constants.unsupported_operation);
  }

  @Override
  public Call createCall(Endpoint caller) {
    return createCall(caller, null);
  }

  @Override
  public Call createCall(Endpoint caller, Map<String, String> headers) {
    return new OutgoingCallImpl(_mohoRemote, null, (CallableEndpoint) caller, this, headers);
  }

  @Override
  public Call createCall(String caller) {
    CallableEndpoint ed = (CallableEndpoint) _mohoRemote.createEndpoint(URI.create(caller));
    return createCall(ed);
  }

  @Override
  public Call createCall(String caller, Map<String, String> headers) {
    CallableEndpoint ed = (CallableEndpoint) _mohoRemote.createEndpoint(URI.create(caller));
    return createCall(ed, headers);
  }
}

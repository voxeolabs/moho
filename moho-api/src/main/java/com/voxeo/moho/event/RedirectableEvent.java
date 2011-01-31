package com.voxeo.moho.event;

import java.util.Map;

import com.voxeo.moho.Endpoint;
import com.voxeo.moho.SignalException;

public interface RedirectableEvent {

  boolean isRedirected();

  void redirect(final Endpoint other) throws SignalException, IllegalArgumentException;

  void redirect(Endpoint other, Map<String, String> headers) throws SignalException, IllegalArgumentException;

}

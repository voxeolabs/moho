package com.voxeo.moho.sample;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.State;
import com.voxeo.moho.TextableEndpoint;
import com.voxeo.moho.event.RegisterEvent;
import com.voxeo.moho.event.TextEvent;

public class Echo implements Application {

  private Map<String, Endpoint> addresses = new ConcurrentHashMap<String, Endpoint>();

  public void destroy() {

  }

  public void init(final ApplicationContext ctx) {
  }

  @State
  public void handleRegister(final RegisterEvent evt) {
    if (evt.getExpiration() > 0) {
      addresses.put(evt.getEndpoint().getURI(), evt.getContacts()[0]);
    }
    else {
      addresses.remove(evt.getEndpoint().getURI());
    }
    evt.accept();
  }

  @State
  public void handleText(final TextEvent evt) throws Throwable {
    ((TextableEndpoint) addresses.get(evt.getSource().getURI())).sendText(evt.getDestination(), evt.getText(), evt
        .getTextType());
  }
}

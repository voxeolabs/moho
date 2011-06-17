package com.voxeo.moho.sample;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.State;
import com.voxeo.moho.TextableEndpoint;
import com.voxeo.moho.event.RegisterEvent;
import com.voxeo.moho.event.RegisterEvent.Contact;
import com.voxeo.moho.event.TextEvent;

public class Echo implements Application {

  private Map<URI, Endpoint> addresses = new ConcurrentHashMap<URI, Endpoint>();

  public void destroy() {

  }

  public void init(final ApplicationContext ctx) {
  }

  @State
  public void handleRegister(final RegisterEvent evt) {
    Contact contact = evt.getContacts()[0];
    if (contact.getExpiration() > 0) {
      addresses.put(evt.getEndpoint().getURI(), contact.getEndpoint());
    }
    else {
      addresses.remove(evt.getEndpoint().getURI());
    }
    evt.accept();
  }


  @State
  public void handleText(final TextEvent evt) throws Throwable {
    ((TextableEndpoint) addresses.get(evt.getFrom().getURI())).sendText(evt.getTo(), evt.getText(), evt.getTextType());
  }
}

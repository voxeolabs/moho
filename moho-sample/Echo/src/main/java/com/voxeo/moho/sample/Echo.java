package com.voxeo.moho.sample;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.State;
import com.voxeo.moho.TextableEndpoint;
import com.voxeo.moho.event.RegisterEvent;
import com.voxeo.moho.event.TextEvent;
import com.voxeo.moho.imified.ImifiedEndpoint;

public class Echo implements Application {

  private Map<String, Endpoint> addresses = new ConcurrentHashMap<String, Endpoint>();

  private ApplicationContext _ctx = null;

  public void destroy() {

  }

  public void init(final ApplicationContext ctx) {
    _ctx = ctx;
  }

  @State()
  public void register(final RegisterEvent ev) throws SignalException {
    if (ev.getExpiration() > 0) {
      for (final Endpoint contact : ev.getContacts()) {
        addresses.put(ev.getEndpoint().getURI().toLowerCase(), contact);
      }
    }
    else {
      for (final Endpoint contact : ev.getContacts()) {
        addresses.remove(ev.getEndpoint().getURI().toLowerCase());
      }
    }

    ev.accept();
  }

  @State
  public void handleText(final TextEvent e) {
    final String message = e.getText();
    final String type = e.getTextType();
    if (e.getDestination() instanceof ImifiedEndpoint) {
      final ImifiedEndpoint ie = (ImifiedEndpoint) e.getDestination();

      ie.setImifiedUserName("zxpzlp@hotmail.com");
      ie.setImifiedPasswd("wzhu");
      try {
        e.getSource().sendText(ie, message, type);
      }
      catch (final Exception ex) {
        ex.printStackTrace();
      }
    }
    else {
      try {
        if (addresses.get(e.getSource().getURI().toLowerCase()) != null) {
          ((TextableEndpoint) _ctx.getEndpoint(addresses.get(e.getSource().getURI().toLowerCase()).getURI())).sendText(
              e.getDestination(), message, type);
        }
      }
      catch (final Exception ex) {
        ex.printStackTrace();
      }

    }
  }
}

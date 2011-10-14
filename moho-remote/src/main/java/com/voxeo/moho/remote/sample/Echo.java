package com.voxeo.moho.remote.sample;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.voxeo.moho.Endpoint;
import com.voxeo.moho.State;
import com.voxeo.moho.TextableEndpoint;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.event.RegisterEvent;
import com.voxeo.moho.event.TextEvent;
import com.voxeo.moho.event.RegisterEvent.Contact;
import com.voxeo.moho.remote.MohoRemote;
import com.voxeo.moho.remote.impl.MohoRemoteImpl;

public class Echo implements Observer {

  private Map<URI, Endpoint> addresses = new ConcurrentHashMap<URI, Endpoint>();
  
  public static void main(String[] args) {
    MohoRemote mohoRemote = new MohoRemoteImpl();
    mohoRemote.addObserver(new Echo());
    mohoRemote.connect(new SimpleAuthenticateCallbackImpl("usera", "1", "", "voxeo"), "localhost", "localhost");
    try {
      Thread.sleep(100 * 60 * 1000);
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
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

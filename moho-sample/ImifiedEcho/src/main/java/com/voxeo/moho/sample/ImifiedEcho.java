package com.voxeo.moho.sample;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.State;
import com.voxeo.moho.event.TextEvent;
import com.voxeo.moho.imified.ImifiedEndpoint;

public class ImifiedEcho implements Application {

  public void destroy() {

  }

  public void init(final ApplicationContext ctx) {

  }

  @State
  public void handleText(final TextEvent e) throws Exception {
      final ImifiedEndpoint endpoint = (ImifiedEndpoint) e.getDestination();
      endpoint.setImifiedUserName("zxpzlp@hotmail.com");
      endpoint.setImifiedPasswd("wzhu");
      e.getSource().sendText(endpoint,  e.getText(), e.getTextType());
  }
}

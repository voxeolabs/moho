package com.voxeo.moho.sample;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.State;
import com.voxeo.moho.event.TextEvent;
import com.voxeo.moho.text.imified.ImifiedTextChannelProvider;
import com.voxeo.moho.textchannel.TextChannels;
import com.voxeo.moho.textchannel.imified.ImifiedEndpoint;

public class ImifiedEcho implements Application {
  private String _name;

  private String _pwd;

  public void destroy() {

  }

  public void init(final ApplicationContext ctx) {
    _name = ctx.getParameter("username");
    _pwd = ctx.getParameter("password");

    TextChannels.registerProvider(new ImifiedTextChannelProvider());
  }

  @State
  public void handleText(final TextEvent e) throws Exception {
    final ImifiedEndpoint endpoint = (ImifiedEndpoint) e.getTo();
    if (_name != null) {
      endpoint.setImifiedUserName(_name);
      if (_pwd != null) {
        endpoint.setImifiedPasswd(_pwd);
      }
    }
    e.getFrom().sendText(endpoint, e.getText(), e.getTextType());
  }
}

package com.voxeo.moho.text.imified;

import java.util.Map;

import com.voxeo.moho.SignalException;
import com.voxeo.moho.TextableEndpoint;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.imified.ImifiedTextEvent;

public class IMifiedTextEventImpl extends ImifiedTextEvent {

  public IMifiedTextEventImpl(EventSource source, String channel, String botkey, String userkey, String user,
      String network, String msg, String step, String to, Map<String, String> historyValues) {
    super(source, channel, botkey, userkey, user, network, msg, step, to, historyValues);

  }

  @Override
  public TextableEndpoint getSource() {
    ImifiedEndpointImpl endPoint = new ImifiedEndpointImpl(source.getApplicationContext(), _userkey);
    endPoint.setNetwork(_network);
    endPoint.setAddress(_user);
    return endPoint;
  }

  @Override
  public TextableEndpoint getDestination() {
    ImifiedEndpointImpl endPoint = new ImifiedEndpointImpl(source.getApplicationContext(), _botkey);
    endPoint.setNetwork(_network);
    endPoint.setAddress(_to);
    return endPoint;
  }

  @Override
  public void accept() throws SignalException, IllegalStateException {
    // DO NOTHING
  }

  @Override
  public void accept(Map<String, String> headers) throws SignalException, IllegalStateException {
    // DO NOTHING
  }
}

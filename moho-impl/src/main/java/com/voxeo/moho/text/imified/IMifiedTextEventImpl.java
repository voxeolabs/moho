package com.voxeo.moho.text.imified;

import java.util.Map;

import com.voxeo.moho.SignalException;
import com.voxeo.moho.TextableEndpoint;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.imified.ImifiedTextEvent;

public class IMifiedTextEventImpl extends ImifiedTextEvent {

  public IMifiedTextEventImpl(final EventSource source, final String channel, final String botkey,
      final String userkey, final String user, final String network, final String msg, final String step,
      final String to, final Map<String, String> historyValues) {
    super(source, channel, botkey, userkey, user, network, msg, step, to, historyValues);

  }

  @Override
  public TextableEndpoint getSource() {
    final ImifiedEndpointImpl endPoint = new ImifiedEndpointImpl(source.getApplicationContext(), _userkey);
    endPoint.setNetwork(_network);
    endPoint.setAddress(_user);
    return endPoint;
  }

  @Override
  public TextableEndpoint getDestination() {
    final ImifiedEndpointImpl endPoint = new ImifiedEndpointImpl(source.getApplicationContext(), _botkey);
    endPoint.setNetwork(_network);
    endPoint.setAddress(_to);
    return endPoint;
  }

  @Override
  public void accept() throws SignalException, IllegalStateException {
    // DO NOTHING
  }

  @Override
  public void accept(final Map<String, String> headers) throws SignalException, IllegalStateException {
    // DO NOTHING
  }

}

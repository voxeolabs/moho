package com.voxeo.moho.imified;

import java.util.Map;

import com.voxeo.moho.Framework;
import com.voxeo.moho.TextableEndpoint;
import com.voxeo.moho.event.MohoTextEvent;
import com.voxeo.moho.imified.IMifiedEvent;

public class IMifiedEventImpl extends MohoTextEvent<Framework> implements IMifiedEvent {

  protected String _channel;

  protected String _botkey;

  protected String _userkey;

  protected String _user;

  protected String _network;

  protected String _msg;

  protected String _step;

  protected Map<String, String> _historyValues;

  protected String _to;
  
  protected IMifiedDriver _driver;

  public IMifiedEventImpl(final Framework source, final IMifiedDriver driver, final String channel, final String botkey, final String userkey,
      final String user, final String network, final String msg, final String step, final String to,
      final Map<String, String> historyValues) {
    super(source);
    _channel = channel;
    _botkey = botkey;
    _userkey = userkey;
    _user = user;
    _network = network;
    _msg = msg;
    _step = step;
    _historyValues = historyValues;
    _to = to;
    _driver = driver;
  }

  @Override
  public String getText() {
    return _msg;
  }

  @Override
  public String getTextType() {
    return null;
  }

  @Override
  public Map<String, String> getHistory() {
    return _historyValues;
  }

  @Override
  public String getChannel() {
    return _channel;
  }

  @Override
  public String getBotkey() {
    return _botkey;
  }

  @Override
  public String getUserkey() {
    return _userkey;
  }

  @Override
  public String getFromUser() {
    return _user;
  }

  @Override
  public String getNetwork() {
    return _network;
  }

  @Override
  public String getStep() {
    return _step;
  }

  @Override
  public String getToUser() {
    return _to;
  }

  @Override
  public TextableEndpoint getFrom() {
    final ImifiedEndpointImpl endPoint = new ImifiedEndpointImpl(_driver, _userkey);
    endPoint.setNetwork(_network);
    endPoint.setAddress(_user);
    return endPoint;
  }

  @Override
  public TextableEndpoint getTo() {
    final ImifiedEndpointImpl endPoint = new ImifiedEndpointImpl(_driver, _botkey);
    endPoint.setNetwork(_network);
    endPoint.setAddress(_to);
    return endPoint;
  }

}

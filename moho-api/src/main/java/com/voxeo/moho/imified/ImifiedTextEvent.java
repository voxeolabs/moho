package com.voxeo.moho.imified;

import java.util.Map;

import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.TextEvent;

public abstract class ImifiedTextEvent extends TextEvent {

  protected String _channel;

  protected String _botkey;

  protected String _userkey;

  protected String _user;

  protected String _network;

  protected String _msg;

  protected String _step;

  protected Map<String, String> _historyValues;

  protected String _to;

  public ImifiedTextEvent(EventSource source, String channel, String botkey, String userkey, String user,
      String network, String msg, String step, String to, Map<String, String> historyValues) {
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
  }

  @Override
  public String getText() {
    return _msg;
  }

  public Map<String, String> getHistory() {
    return _historyValues;
  }

  public String getChannel() {
    return _channel;
  }

  public String getBotkey() {
    return _botkey;
  }

  public String getUserkey() {
    return _userkey;
  }

  public String getUser() {
    return _user;
  }

  public String getNetwork() {
    return _network;
  }

  public String getStep() {
    return _step;
  }

  public Map<String, String> getHistoryValues() {
    return _historyValues;
  }

  public String getTo() {
    return _to;
  }
}

package com.voxeo.moho.presence.sip;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.sip.SipServletRequest;

import com.voxeo.moho.presence.NotifyBody;
import com.voxeo.moho.presence.PresenceFactory;
import com.voxeo.moho.presence.sip.impl.SIPPresenceResourceImpl;
import com.voxeo.moho.presence.sip.impl.notifybody.PIDFNotifyBody;
import com.voxeo.moho.spi.ExecutionContext;

public class SipPresenceFactory implements PresenceFactory {
  
  protected static final Map<String, String> DEFAULT_NOTIFY_BODY = new HashMap<String, String>();
  
  static {
    DEFAULT_NOTIFY_BODY.put("presence", "application/pidf+xml");
  }
  
  protected int _subscribeDefaultExpires = 3600;

  protected int _subscribeMaxExpires = 3600000;

  protected int _subscribeMinExpires = 30;

  protected int _publishDefaultExpires = 300;

  protected int _publishMaxExpires = 3000;

  protected int _publishMinExpires = 5;
  
  private ExecutionContext _contex;

  @Override
  public SIPResource createResource(String resourceUri, String eventName) {
    if ("presence".equalsIgnoreCase(eventName)) {
      return new SIPPresenceResourceImpl(_contex, resourceUri, eventName);
    }
    else {
      throw new IllegalArgumentException("Unsupported event[" + eventName + "]");
    }
  }
  
  public NotifyBody createNotifyBody(SipServletRequest req) throws IOException {
    String eventName = req.getHeader("Event");
    if ("presence".equalsIgnoreCase(eventName)) {
      return new PIDFNotifyBody(req.getCharacterEncoding(), req.getRawContent());
    }
    else {
      throw new IllegalArgumentException("Unsupported event[" + eventName + "]");
    }
  }
  
  public String getDefaultNotifyBodyName(String eventName) {
    if (DEFAULT_NOTIFY_BODY.containsKey(eventName)) {
      return DEFAULT_NOTIFY_BODY.get(eventName);
    }
    throw new IllegalArgumentException("Can't find default notifyBodyType for event[" + eventName + "]");
  }
  
  
  /**
   * @param expires
   * @return a compliant expire value, if -1, the expire value is too brief
   */
  public int checkSubscribeExpires(int expires) {
    int retv = expires;
    if (retv < 0) {
      retv = getSubscribeDefaultExpires();
    }
    if (retv > getSubscribeMaxExpires()) {
      retv = getSubscribeMaxExpires();
    }
    if (retv > 0 && retv < 3600 && retv < getSubscribeMinExpires()) {
      return -1;
    }
    return retv;
  }
  
  /**
   * @param expires
   * @return a compliant expire value, if -1, the expire value is too brief
   */
  public int checkPublishExpires(int expires) {
    if (expires < 0) {
      expires = getPublishDefaultExpires();
    }
    if (expires > getPublishMaxExpires()) {
      expires = getPublishMaxExpires();
    }

    if (expires > 0 && expires < getPublishMinExpires()) {
      return -1;
    }
    return expires;
  }
  
  public int getSubscribeDefaultExpires() {
    return _subscribeDefaultExpires;
  }

  public void setSubscribeDefaultExpires(int subscribeDefaultExpires) {
    _subscribeDefaultExpires = subscribeDefaultExpires;
  }

  public int getSubscribeMaxExpires() {
    return _subscribeMaxExpires;
  }

  public void setSubscribeMaxExpires(int subscribeMaxExpires) {
    _subscribeMaxExpires = subscribeMaxExpires;
  }

  public int getSubscribeMinExpires() {
    return _subscribeMinExpires;
  }

  public void setSubscribeMinExpires(int subscribeMinExpires) {
    _subscribeMinExpires = subscribeMinExpires;
  }

  public int getPublishDefaultExpires() {
    return _publishDefaultExpires;
  }

  public void setPublishDefaultExpires(int publishDefaultExpires) {
    _publishDefaultExpires = publishDefaultExpires;
  }

  public int getPublishMaxExpires() {
    return _publishMaxExpires;
  }

  public void setPublishMaxExpires(int publishMaxExpires) {
    _publishMaxExpires = publishMaxExpires;
  }

  public int getPublishMinExpires() {
    return _publishMinExpires;
  }

  public void setPublishMinExpires(int publishMinExpires) {
    _publishMinExpires = publishMinExpires;
  }

  @Override
  public void init(ExecutionContext context, Map<String, String> properties) throws Exception {
    _contex = context;
    if (properties.get(SIPPresenceService.MAX_EXPIRE) != null) {
      _subscribeMaxExpires = Integer.parseInt(properties.get(SIPPresenceService.MAX_EXPIRE));
    }
    if (properties.get(SIPPresenceService.MIN_EXPIRE) != null) {
      _subscribeMinExpires = Integer.parseInt(properties.get(SIPPresenceService.MIN_EXPIRE));
    }
  }

  @Override
  public void destroy() {
    
  }

  @Override
  public String getName() {
    return SipPresenceFactory.class.getName();
  }
}

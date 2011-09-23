package com.voxeo.moho.presence.sip.impl;

import java.util.ListIterator;

import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipSessionsUtil;
import javax.servlet.sip.URI;

import org.apache.log4j.Logger;

import com.voxeo.moho.presence.impl.StoreHolder;
import com.voxeo.moho.presence.sip.SipSubscriptionState;
import com.voxeo.moho.sip.SIPSubscribeEvent.SIPSubscriptionContext;
import com.voxeo.moho.spi.ExecutionContext;

public class SIPSubscriptionContextImpl implements SIPSubscriptionContext {
  
  private static final long serialVersionUID = 5166897019787952683L;
  
  private static final Logger LOG = Logger.getLogger(SIPSubscriptionContextImpl.class);

  private String _subscriber;
  
  private String _subscribee;
  
  private String _eventName;
  
  private String _notifyBodyType;
  
  private int _expires;
  
  private transient SipSession _dialog;
  
  private transient SipSessionsUtil _sessionUtil;
  
  private transient ExecutionContext _context;
  
  private SipSubscriptionState _state;
  
  private SIPSubscriptionID _id;

  private long _updateTime;

  private String _appId;
  
  public SIPSubscriptionContextImpl(URI from, URI to, SipServletRequest request) {
    _subscriber = Utils.getCleanUri(from).toString();
    _subscribee = Utils.getCleanUri(to).toString();
    _dialog = request.getSession();
    _sessionUtil = (SipSessionsUtil) _dialog.getServletContext().getAttribute(SipServlet.SIP_SESSIONS_UTIL);
    _appId = _dialog.getApplicationSession().getId();
    EventHeader eventHeader = new EventHeader(request.getHeader("Event"));
    _id = new SIPSubscriptionID(_dialog.getId(), eventHeader.getEventId());
    _eventName = eventHeader.getEventName();
    _notifyBodyType = parseNotifyBodyType(request.getHeaders("Accept"));
    updateExpires(request.getExpires());
    setState(SipSubscriptionStateImpl.ALLOW);
  }

  private String parseNotifyBodyType(ListIterator<String> notifyBodies) {
    if (SIPConstans.EVENT_NAME_PRESENCE.equals(_eventName)) {
      //TODO
      return SIPConstans.NOTIFY_BODY_PRESENCE;
//      for (; notifyBodies.hasNext();) {
//        String bodyType = notifyBodies.next();
//        if (bodyType.trim().equals(SIPConstans.NOTIFY_BODY_PRESENCE)) {
//          return bodyType;
//        }
//      }
//      throw new IllegalArgumentException("Unsupported notify body type");
    }
    return notifyBodies.next();
  }

  @Override
  public String getSubscriber() {
    return _subscriber;
  }

  @Override
  public String getSubscribee() {
    return _subscribee;
  }

  @Override
  public String getEventName() {
    return _eventName;
  }

  @Override
  public String getNotifyBodyType() {
    return _notifyBodyType;
  }

  @Override
  public int getExpires() {
    return _expires;
  }

  public void setExpires(int expires) {
    _expires = expires;
  }
  
  public void setUpdateTime(long updateDate) {
    _updateTime = updateDate;
  }
  
  public void updateExpires(int expires) {
    setExpires(expires);
    setUpdateTime(System.currentTimeMillis());
  }
  
  public int getSpareTime() {
    long temp = _updateTime + _expires * 1000 - System.currentTimeMillis();
    temp = temp / 1000;
    return (int) ((temp > 0) ? temp : 0);
  }
  
  @Override
  public Runnable sendNotify() {
    return new NotifyRequest(this, StoreHolder.getPresenceStore());
  }

  public SipSubscriptionState getState() {
    return _state;
  }

  public void setState(SipSubscriptionState state) {
    _state = state;
  }

  /**
   * there two situations the session could be null<br>
   * 1. after the sipoint server abnormally restart, all sessions have been
   * automatically expired <br>
   * 2. in a cluster enviroment, the seesion has not been duplicated fron other server.
   */
  public SipSession getDialog() {
    try {
      if (_dialog == null && _id.getSessionId() != null && _appId != null) {
        if (_sessionUtil == null) {
          _sessionUtil = (SipSessionsUtil) _context.getServletContext().getAttribute(SipServlet.SIP_SESSIONS_UTIL);
        }
        SipApplicationSession sas = (SipApplicationSession) _sessionUtil.getApplicationSessionById(_appId);
        _dialog = sas.getSipSession(_id.getSessionId());
      }
      return _dialog;
    }
    catch (NullPointerException e) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("", e);
      }
      // LOG.warn("Invalid subscribe session [" + getAppId() + "," + getSessionId() + "]. " + e);
      return null;
    }
    catch (IllegalStateException e) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Not running in container ", e);
      }
      // LOG.warn("Invalid subscribe session [" + getAppId() + "," + getSessionId() + "]. " + e);
      return null;
    }
  }

  @Override
  public Object getId() {
    return _id;
  }

  @Override
  public String toString() {
    return "SIPSubscriptionContext [_subscriber=" + _subscriber + ", _subscribee=" + _subscribee + ", _eventName="
        + _eventName + ", _notifyBodyType=" + _notifyBodyType + ", _expires=" + _expires + ", _state=" + _state + "]";
  }

  @Override
  public void setExecutionContext(ExecutionContext context) {
    _context = context;
  }
}

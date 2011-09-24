package com.voxeo.moho.presence.sip.impl;

import java.io.Serializable;

import com.voxeo.moho.presence.SubscriptionID;


public class SIPSubscriptionID implements Serializable, SubscriptionID {
  
  private static final long serialVersionUID = -4364719961104164552L;

  protected String _eventId = EventHeader.NULLEVENT;

  protected String _sessionId;

  public SIPSubscriptionID() {
     _eventId = EventHeader.NULLEVENT;
  }

  public SIPSubscriptionID(String sid) {
    _eventId = EventHeader.NULLEVENT;
    _sessionId = sid;
  }

  public SIPSubscriptionID(String sid, String eid) {
    _sessionId = sid;
    setEventId(eid);
  }

  public String getEventId() {
    return _eventId;
  }

  public void setEventId(String eventId) {
    if (eventId != null) {
      _eventId = eventId;
    }
    else {
      _eventId = EventHeader.NULLEVENT;
    }
  }

  public String getSessionId() {
    return _sessionId;
  }

  public void setSessionId(String id) {
    _sessionId = id;
  }

  public String toString() {
    return "SIPSubscriptionID [sessionid=" + _sessionId + ",eventid=" + _eventId + "]";
  }

  public int hashCode() {
    return _sessionId.hashCode() + _eventId.hashCode();
  }

  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof SIPSubscriptionID)) {
      return false;
    }
    SIPSubscriptionID obj1 = (SIPSubscriptionID) obj;
    if (!_eventId.equals(obj1._eventId)) {
      return false;
    }
    if (!_sessionId.equals(obj1._sessionId)) {
      return false;
    }
    return true;
  }
}

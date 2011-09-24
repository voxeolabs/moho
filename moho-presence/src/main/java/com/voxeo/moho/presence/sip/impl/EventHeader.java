package com.voxeo.moho.presence.sip.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * Convert a String like "ua-profile;profile-type=application;auid="pres-rules""
 * to a utility object with a eventName as a string and all parameters as a map.
 */
public class EventHeader {

  public static final String NULLEVENT = "nullevent";

  protected String m_eventName;
  
  protected String _s;

  protected Map<String, String> m_paras = new HashMap<String, String>();

  public EventHeader(String eventHeader) {
    if (StringUtils.isEmpty(eventHeader)) {
      return;
    }
    _s = eventHeader;
    String[] eventHeaderArr = StringUtils.split(eventHeader, ';');
    setEventName(eventHeaderArr[0]);
    for (int i = 1; i < eventHeaderArr.length; i++) {
      String[] paras = StringUtils.split(eventHeaderArr[i], '=');
      m_paras.put(paras[0].trim(), paras[1].trim());
    }
  }

  public String getEventId() {
    String eid = getParameter("id");
    if (eid == null) {
      return NULLEVENT;
    }
    return eid;
  }

  public void setEventId(String eventId) {
    setParameter("id", eventId);
  }

  public String getEventName() {
    return m_eventName;
  }

  public void setEventName(String eventName) {
    // convert the event name to the system constants
    this.m_eventName = Utils.staticEventNameStringAddress(eventName);
  }

  public String getParameter(String name) {
    return m_paras.get(name);
  }

  public void setParameter(String name, String value) {
    m_paras.put(name, value);
  }
  
  public Map<String, String> getAllParameters() {
    return m_paras;
  }
  
  public String toString() {
    return _s;
  }
}

package com.voxeo.moho.presence.sip.impl;

import com.voxeo.moho.presence.NotifyBody;
import com.voxeo.moho.presence.sip.EventSoftState;

public class SIPEventSoftState implements EventSoftState {
  private static final long serialVersionUID = 1789359860278842387L;

  protected String m_resourceURL;

  protected String m_eventName;

  /*
   * e-tag
   * sip-if-match
   */
  protected String m_beforeEntityTag;
  
  /*
   * e-tag
   * sip-if-match
   */
  protected String m_entityTag;

  protected int m_expires = 0;

  protected long m_updateTime;

  protected String m_contentType;

  // this is a temporary field, not saved in the database
  protected transient NotifyBody m_body;
  //protected SoftStateBlobItem m_bodyItem;

  public SIPEventSoftState() {
  }
  
  public SIPEventSoftState(String resourceURL, String eventName, int expires,
      String contentType, NotifyBody body) {
    m_resourceURL = resourceURL;
    m_eventName = eventName;
    m_expires = expires;
    m_contentType = contentType;
    setBody(body);
    m_updateTime = System.currentTimeMillis();
    m_entityTag = String.valueOf(m_updateTime);
  }

  @Override
  public String getEventName() {
    return m_eventName;
  }

  public void setEventName(String eventName) {
    m_eventName = eventName;
  }

  @Override
  public String getContentType() {
    return m_contentType;
  }

  public void setContentType(String notifyBodyName) {
    m_contentType = notifyBodyName;
  }

  @Override
  public String getResourceURL() {
    return m_resourceURL;
  }

  public void setResourceURL(String resourceURL) {
    m_resourceURL = resourceURL;
  }

  @Override
  public String getEntityTag() {
    return m_entityTag;
  }
  
  @Override
  public String getBeforeEntityTag() {
    return m_beforeEntityTag;
  }

  public void setBeforeEntityTag(String entityTag) {
    m_beforeEntityTag = entityTag;
  }
  
  public void setEntityTag(String entityTag) {
   m_entityTag = entityTag;
  }
   
  public void setUpdateTime(long time) {
    m_updateTime = time;
  }

  @Override
  public long getUpdateTime() {
    return m_updateTime;
  }
  
  @Override
  public void updateExpires(int expires) {
    m_expires = expires;
    m_updateTime = System.currentTimeMillis();
    m_beforeEntityTag = m_entityTag;
    m_entityTag = String.valueOf(m_updateTime);
  }

  public int getExpires() {
    return m_expires;
  }

  @Override
  public boolean isExpired() {
    return (getSpareTime() <= 0);
  }

  @Override
  public int getSpareTime() {
    long temp = m_updateTime + m_expires * 1000 - System.currentTimeMillis();
    temp = temp / 1000;
    return (int) ((temp > 0) ? temp : 0);
  }

  public NotifyBody getBody() {
    return m_body;
  }

  public void setBody(NotifyBody body) {
    m_body = body;
  }

  @Override
  public String toString() {
    return "SIPEventSoftState [m_resourceURL=" + m_resourceURL + ", m_eventName=" + m_eventName
        + ", m_beforeEntityTag=" + m_beforeEntityTag + ", m_entityTag=" + m_entityTag + ", m_expires=" + m_expires
        + ", m_contentType=" + m_contentType + "]";
  }
}

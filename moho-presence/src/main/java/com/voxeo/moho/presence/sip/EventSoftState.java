package com.voxeo.moho.presence.sip;

import java.io.Serializable;

import com.voxeo.moho.presence.NotifyBody;

public interface EventSoftState extends Serializable {

  NotifyBody getBody();
  
  void setBody(NotifyBody body);

  int getSpareTime();

  boolean isExpired();

  void updateExpires(int expires);

  long getUpdateTime();

  String getBeforeEntityTag();

  String getEntityTag();

  String getResourceURL();

  String getContentType();

  String getEventName();

}

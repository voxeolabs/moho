package com.voxeo.moho.presence.sip;


public interface SIPPresenceResource extends SIPResource {
  
  void addEventSoftState(EventSoftState softState);
  
  void refreshEventSoftState(EventSoftState softState);
  
  void updateEventSoftState(EventSoftState softState);
  
  void removeEventSoftState(EventSoftState softState);
  
  EventSoftState getSoftState(String sipIfMatch);
}

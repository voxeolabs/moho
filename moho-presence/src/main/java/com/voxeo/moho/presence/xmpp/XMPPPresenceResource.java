package com.voxeo.moho.presence.xmpp;

import com.voxeo.moho.presence.Resource;

public interface XMPPPresenceResource extends Resource {
  boolean isAvailable();

  boolean isInterested();
  
  void setInterested(boolean interested);
  
  void setAvailable(boolean available);
  
  String getBareJID();
}

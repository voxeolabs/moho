package com.voxeo.moho.presence;

import com.voxeo.moho.services.Service;


public interface PresenceFactory extends Service {
  
  Resource createResource(String resourceUri, String eventName);
  
}

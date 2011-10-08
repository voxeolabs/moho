package com.voxeo.moho.remote.impl;

import com.voxeo.moho.event.EventSource;

public interface MohoRemoteEventSource extends EventSource {

  public MohoRemoteImpl getMohoRemote();

}

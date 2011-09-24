package com.voxeo.moho.presence;

import com.voxeo.moho.presence.sip.impl.NotifyRequest;

public interface NotifyDispatcher extends Runnable {
  void put(NotifyRequest nr) throws InterruptedException;
  
  void shutdown();
}

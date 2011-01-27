package com.voxeo.moho.event;

public abstract class EarlyMediaEvent extends SignalEvent implements RejectableEvent {

  protected EarlyMediaEvent(EventSource source) {
    super(source);
  }

}

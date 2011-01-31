package com.voxeo.moho.event;

public abstract class EarlyMediaEvent extends SignalEvent implements RejectableEvent {

  protected boolean _rejected = false;

  protected EarlyMediaEvent(final EventSource source) {
    super(source);
  }

  @Override
  public boolean isRejected() {
    return _rejected;
  }

  @Override
  public boolean isProcessed() {
    return isAccepted() || isRejected();
  }

}

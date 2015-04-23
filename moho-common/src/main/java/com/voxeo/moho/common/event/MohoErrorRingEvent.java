package com.voxeo.moho.common.event;

import com.voxeo.moho.Call;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.event.ErrorRingEvent;

public abstract class MohoErrorRingEvent extends MohoCallEvent implements ErrorRingEvent {

  protected boolean _rejected = false;

  protected boolean _accepted = false;

  protected MohoErrorRingEvent(final Call source) {
    super(source);
  }

  @Override
  public boolean isAccepted() {
    return _accepted;
  }

  @Override
  public void accept() throws SignalException {
    this.accept(null);
  }

  @Override
  public void reject(Reason reason) throws SignalException {
    this.reject(reason, null);
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

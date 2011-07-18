package com.voxeo.moho.event;

import com.voxeo.moho.Call;
import com.voxeo.moho.SignalException;

public abstract class MohoEarlyMediaEvent extends MohoCallEvent implements EarlyMediaEvent {

  protected boolean _rejected = false;
  protected boolean _accepted = false;

  protected MohoEarlyMediaEvent(final Call source) {
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

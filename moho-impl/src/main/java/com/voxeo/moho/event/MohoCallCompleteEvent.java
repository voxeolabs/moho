package com.voxeo.moho.event;

import com.voxeo.moho.Call;

public class MohoCallCompleteEvent extends MohoCallEvent implements CallCompleteEvent {

  protected Cause _cause;

  protected Exception _exception;

  public MohoCallCompleteEvent(final Call source, final Cause cause) {
    super(source);
    _cause = cause;
  }

  public MohoCallCompleteEvent(final Call source, final Cause cause, final Exception e) {
    super(source);
    _cause = cause;
    _exception = e;
  }

  public Cause getCause() {
    return _cause;
  }
  
  public Exception getException() {
	  
	  return _exception;
  }

  @Override
  public boolean isProcessed() {
    return true;
  }
}

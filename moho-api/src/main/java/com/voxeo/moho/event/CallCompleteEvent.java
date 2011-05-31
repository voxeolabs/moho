package com.voxeo.moho.event;

import com.voxeo.moho.Call;
import com.voxeo.moho.utils.Event;

public class CallCompleteEvent extends Event<Call> {

  public enum Cause {
    DISCONNECT, CANCEL, BUSY, DECLINE, FORBIDDEN, TIMEOUT, ERROR, NEAR_END_DISCONNECT, REDIRECT
  }

  protected Cause _cause;

  protected Exception _exception;

  public CallCompleteEvent(final Call source, final Cause cause) {
    super(source);
    _cause = cause;
  }

  public CallCompleteEvent(final Call source, final Cause cause, final Exception e) {
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
}

package com.voxeo.moho.event;

import com.voxeo.utils.Event;

public class CallCompleteEvent extends Event<EventSource> {

  public enum Cause {
    DISCONNECT, CANCEL, BUSY, DECLINE, FORBIDDEN, TIMEOUT, ERROR, NEAR_END_DISCONNECT
  }

  protected Cause _cause;

  protected Exception _exception;

  public CallCompleteEvent(final EventSource source, final Cause cause) {
    super(source);
    _cause = cause;
  }

  public CallCompleteEvent(final EventSource source, final Cause cause, final Exception e) {
    super(source);
    _cause = cause;
    _exception = e;
  }

  public Cause getCause() {
    return _cause;
  }
}

package com.voxeo.moho.common.event;

import com.voxeo.moho.event.Event;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.UncaughtExceptionEvent;

public class UncaughtExceptionEventImpl<T extends EventSource> extends MohoEvent<T> implements UncaughtExceptionEvent<T> {
  protected Exception _exception;
  protected Event<T> _event;
  
  public UncaughtExceptionEventImpl(T source, Exception e, Event<T> evt) {
    super(source);
    _exception = e;
  }

  @Override
  public Exception getException() {
    return _exception;
  }
  
  @Override
  public Event<T> getEvent() {
    return _event;
  }

}

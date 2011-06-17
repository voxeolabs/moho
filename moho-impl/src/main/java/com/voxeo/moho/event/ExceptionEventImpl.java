package com.voxeo.moho.event;

public class ExceptionEventImpl<T extends EventSource> extends MohoEvent<T> implements ExceptionEvent<T> {
  protected Exception _exception;
  
  public ExceptionEventImpl(T source, Exception e) {
    super(source);
    _exception = e;
  }

  @Override
  public Exception getException() {
    return _exception;
  }

}

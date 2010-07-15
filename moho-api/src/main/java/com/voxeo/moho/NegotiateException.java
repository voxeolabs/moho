package com.voxeo.moho;

public class NegotiateException extends MediaException {

  private static final long serialVersionUID = 7311335567523528434L;

  protected Object _cause = null;

  public NegotiateException(final Object cause) {
    _cause = cause;
  }

  public Object getCauseObject() {
    return _cause;
  }

}

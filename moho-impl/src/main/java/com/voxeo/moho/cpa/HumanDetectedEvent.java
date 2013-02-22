package com.voxeo.moho.cpa;

import com.voxeo.moho.event.EventSource;

public class HumanDetectedEvent<T extends EventSource> extends AbstractCPAEvent<T> {

  protected final int _retries;

  public HumanDetectedEvent(final T source, final long duration, final int retries) {
    super(source, duration);
    _retries = retries;
  }

  public int getRetries() {
    return _retries;
  }
}

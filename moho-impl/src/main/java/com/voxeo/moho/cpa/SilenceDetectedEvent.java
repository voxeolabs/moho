package com.voxeo.moho.cpa;

import com.voxeo.moho.event.EventSource;

public class SilenceDetectedEvent<T extends EventSource> extends AbstractCPAEvent<T> {

  protected SilenceDetectedEvent(T source, long duration) {
    super(source, duration);
    // TODO Auto-generated constructor stub
  }

}

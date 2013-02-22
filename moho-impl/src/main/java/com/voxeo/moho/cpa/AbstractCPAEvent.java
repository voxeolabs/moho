package com.voxeo.moho.cpa;

import com.voxeo.moho.common.event.MohoMediaNotificationEvent;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.MediaNotificationEvent;

public abstract class AbstractCPAEvent<T extends EventSource> extends MohoMediaNotificationEvent<T> {

  protected final long _duration;

  protected AbstractCPAEvent(final T source) {
    this(source, -1);
  }

  protected AbstractCPAEvent(final T source, final long duration) {
    super(source);
    _duration = duration;
  }

  public long getDuration() {
    return _duration;
  }
}

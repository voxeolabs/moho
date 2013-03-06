package com.voxeo.moho.common.event;

import com.voxeo.moho.event.CPAEvent;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.media.input.SignalGrammar.Signal;

public class MohoCPAEvent<T extends EventSource> implements CPAEvent<T> {

  protected final T source;

  protected final Type type;

  protected long duration = -1;;

  protected int retries = -1;

  protected Signal signal = null;

  protected MohoCPAEvent(final T source, final Type type) {
    this.source = source;
    this.type = type;
  }

  public MohoCPAEvent(final T source, final Type type, final long duration, final int retries) {
    this(source, type);
    this.duration = duration;
    this.retries = retries;
  }

  public MohoCPAEvent(final T source, final Type type, final Signal signal) {
    this(source, type);
    this.signal = signal;
  }

  @Override
  public T getSource() {
    return source;
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public long getDuration() {
    return duration;
  }

  @Override
  public int getRetries() {
    return retries;
  }

  @Override
  public Signal getSignal() {
    return signal;
  }

  @Override
  public String toString() {
    return String.format("[Event class=%s source=%s id=%s type=%s duration=%s retries=%s signal=%s]", getClass()
        .getName(), (source != null ? source.getClass().getSimpleName() + "[" + source.getId() + "]" : null),
        hashCode(), type, duration, retries, signal);
  }
}
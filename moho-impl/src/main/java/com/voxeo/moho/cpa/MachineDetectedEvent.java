package com.voxeo.moho.cpa;

import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.media.input.SignalGrammar.Signal;

public class MachineDetectedEvent<T extends EventSource> extends AbstractCPAEvent<T> {

  protected final int _retries;

  protected final Signal _signal;

  public MachineDetectedEvent(final T source, final long duration, final int retries) {
    super(source, duration);
    _retries = retries;
    _signal = null;
  }

  public MachineDetectedEvent(final T source, final Signal signal) {
    super(source, -1);
    _retries = -1;
    _signal = signal;
  }

  public int getRetries() {
    return _retries;
  }

  public Signal getSignal() {
    return _signal;
  }
}

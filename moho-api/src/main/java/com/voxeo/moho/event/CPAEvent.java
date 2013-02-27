package com.voxeo.moho.event;

import com.voxeo.moho.media.input.SignalGrammar.Signal;

public interface CPAEvent<T extends EventSource> extends MediaEvent<T> {

  public enum Type {
    HUMAN_DETECTED, MACHINE_DETECTED
  }

  public Type getType();

  public long getDuration();

  public int getRetries();

  public Signal getSignal();
}

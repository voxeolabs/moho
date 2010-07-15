package com.voxeo.moho.event;

import com.voxeo.utils.Event;

public class CallCompleteEvent extends Event<EventSource> {

  public CallCompleteEvent(final EventSource source) {
    super(source);
  }

}

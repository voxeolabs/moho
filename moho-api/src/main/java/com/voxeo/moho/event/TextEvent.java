package com.voxeo.moho.event;

import com.voxeo.moho.TextableEndpoint;
import com.voxeo.utils.Event;

public abstract class TextEvent extends Event<EventSource> implements AcceptableEvent {

  public TextEvent(final EventSource source) {
    super(source);
  }

  public abstract String getText();

  public abstract String getTextType();

  public abstract TextableEndpoint getSource();

  public abstract TextableEndpoint getDestination();
}

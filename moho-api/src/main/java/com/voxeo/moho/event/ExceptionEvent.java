package com.voxeo.moho.event;

public interface ExceptionEvent<T extends EventSource> extends Event<T> {
  Exception getException();
}

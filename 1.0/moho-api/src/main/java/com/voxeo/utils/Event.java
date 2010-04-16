package com.voxeo.utils;

public class Event<Source> {
    public final Source source;
    public Event(Source source) {
        super();
        this.source = source;
    }
    
    @Override
    public String toString() {
        return String.format("[Event class=%s sourceClass=%s]",getClass().getName(),(source != null ? source.getClass().getSimpleName() : null));
    }
}

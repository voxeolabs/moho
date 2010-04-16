package com.voxeo.utils;


public class EnumEvent<T,E extends Enum<E>> extends Event<T> {
    public final E type;
    public EnumEvent(T source, E type) {
        super(source);
        this.type = type;
    }
    
    @Override
    public String toString() {
        return String.format("[EnumEvent class=%s type=%s sourceClass=%s]",getClass().getName(),type,(source != null ? source.getClass().getSimpleName() : null));
    }
}

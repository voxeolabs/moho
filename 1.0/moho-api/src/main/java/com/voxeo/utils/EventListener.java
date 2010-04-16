package com.voxeo.utils;

public interface EventListener<E extends Event<?>> {
    void onEvent(E event);
}

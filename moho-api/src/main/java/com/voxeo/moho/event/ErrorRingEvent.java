package com.voxeo.moho.event;

/**
 * This event is fired when an 18x response with Reason header is received on an
 * outbound {@link com.voxeo.moho.Call Call}.
 */
public interface ErrorRingEvent extends CallEvent, AcceptableEvent {

}

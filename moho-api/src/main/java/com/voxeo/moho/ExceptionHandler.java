package com.voxeo.moho;

import com.voxeo.moho.event.EventSource;
import com.voxeo.utils.Event;


public interface ExceptionHandler {

    public boolean handle(Exception ex, Event<? extends EventSource> event);
    
}

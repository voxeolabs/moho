package com.voxeo.moho.xmpp;

import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.TextEvent;

public interface XMPPMessageEvent<T extends EventSource> extends XMPPEvent<T>, TextEvent<T> {

}

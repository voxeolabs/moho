package com.voxeo.moho.xmpp;

import com.voxeo.moho.Framework;
import com.voxeo.moho.event.TextEvent;

public interface XMPPMessageEvent extends XMPPEvent<Framework>, TextEvent<Framework> {
}

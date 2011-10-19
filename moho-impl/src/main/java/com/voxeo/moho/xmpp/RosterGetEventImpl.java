package com.voxeo.moho.xmpp;

import com.voxeo.moho.Framework;
import com.voxeo.moho.xmpp.RosterGet;
import com.voxeo.servlet.xmpp.IQRequest;

public class RosterGetEventImpl extends RosterEventImpl implements RosterGet {

  public RosterGetEventImpl(Framework framework, IQRequest request) {
    super(framework, request);
  }

}

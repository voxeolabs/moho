package com.voxeo.moho.xmpp;

import com.voxeo.moho.Framework;
import com.voxeo.moho.xmpp.RosterSet;
import com.voxeo.servlet.xmpp.IQRequest;

public class RosterSetEventImpl extends RosterEventImpl implements RosterSet {

  public RosterSetEventImpl(Framework framework, IQRequest request) {
    super(framework, request);
  }

}

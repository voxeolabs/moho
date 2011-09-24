package com.voxeo.rayo.mohoremote.impl;

import com.voxeo.rayo.mohoremote.MohoRemote;
import com.voxeo.rayo.mohoremote.MohoRemoteFactory;

public class MohoRemoteFactoryImpl extends MohoRemoteFactory {

  @Override
  public MohoRemote newMohoRemote() {
    return new MohoRemoteImpl();
  }

}

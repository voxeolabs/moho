package com.voxeo.moho.remote.impl;

import com.voxeo.moho.remote.MohoRemote;
import com.voxeo.moho.remote.MohoRemoteFactory;

@Deprecated
public class MohoRemoteFactoryImpl extends MohoRemoteFactory {

  @Override
  public MohoRemote newMohoRemote() {
    return new MohoRemoteImpl();
  }

}

package com.voxeo.moho.remote.impl;

import java.util.Map;

import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.OutgoingCall;

public class OutgoingCallImpl extends CallImpl implements OutgoingCall {

  protected OutgoingCallImpl(MohoRemoteImpl mohoRemote, String callID, CallableEndpoint caller,
      CallableEndpoint callee, Map<String, String> headers) {
    super(mohoRemote, callID, caller, callee, headers);
  }

}

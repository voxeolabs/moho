package com.voxeo.moho.sip;

import java.util.Map;

import com.voxeo.moho.OutgoingCall;
import com.voxeo.moho.services.Service;

public interface OutgoingCallFactory extends Service {

  OutgoingCall createOutgoingCall(SIPEndpoint from, SIPEndpoint to, Map<String, String> headers);

  OutgoingCall createOutgoingCall(SIPEndpoint from, SIPEndpoint to, Map<String, String> headers, SIPCall originalCall);

}

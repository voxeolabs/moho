package com.voxeo.moho.event.fake;

import java.util.Map;

import com.voxeo.moho.sip.SIPEndpoint;
import com.voxeo.moho.sip.SIPOutgoingCall;
import com.voxeo.moho.spi.ExecutionContext;

public class MockSIPOutgoingCall extends SIPOutgoingCall {

  protected MockSIPOutgoingCall(ExecutionContext context, SIPEndpoint from, SIPEndpoint to, Map<String, String> headers) {
    super(context, from, to, headers);
    // TODO Auto-generated constructor stub
  }
  
  

}

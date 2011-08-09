package com.voxeo.moho.sip;

import java.util.Map;

import com.voxeo.moho.OutgoingCall;
import com.voxeo.moho.spi.ExecutionContext;

public class OutgoingCallFactoryImpl implements OutgoingCallFactory {

  ExecutionContext _context;

  @Override
  public void init(ExecutionContext context, Map<String, String> properties) throws Exception {
    _context = context;
  }

  @Override
  public void destroy() {

  }

  @Override
  public OutgoingCall createOutgoingCall(SIPEndpoint from, SIPEndpoint to, Map<String, String> headers) {
    return new SIPOutgoingCall(_context, from, to, headers);
  }

  @Override
  public String getName() {
    return OutgoingCallFactory.class.getName();
  }

}

package com.voxeo.moho.sip;

import javax.servlet.sip.SipServletRequest;

import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.services.Service;

public interface IncomingCallFactory extends Service {

  IncomingCall createIncomingCall(SipServletRequest request);
}

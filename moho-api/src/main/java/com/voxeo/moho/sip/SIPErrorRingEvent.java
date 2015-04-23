package com.voxeo.moho.sip;

import javax.servlet.sip.SipServletResponse;

import com.voxeo.moho.event.ErrorRingEvent;

/**
 * SIP specific {@link com.voxeo.moho.event.ErrorRingEvent ErrorRingEvent}.
 */
public interface SIPErrorRingEvent extends ErrorRingEvent {
  /**
   * @return SIP response.
   */
  SipServletResponse getSipResponse();
}

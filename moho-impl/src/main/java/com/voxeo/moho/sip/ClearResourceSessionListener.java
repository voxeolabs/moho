package com.voxeo.moho.sip;

import javax.servlet.sip.SipSessionEvent;
import javax.servlet.sip.SipSessionListener;
import javax.servlet.sip.annotation.SipListener;

import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.util.SessionUtils;

@SipListener
public class ClearResourceSessionListener implements SipSessionListener {

  @Override
  public void sessionCreated(SipSessionEvent sipsessionevent) {

  }

  @Override
  public void sessionDestroyed(SipSessionEvent sipsessionevent) {
    final EventSource source = SessionUtils.getEventSource(sipsessionevent.getSession());

    if (source != null && source instanceof SIPCallImpl) {
      ((SIPCallImpl) source).disconnect(true, CallCompleteEvent.Cause.ERROR, null, null);
    }
  }

  @Override
  public void sessionReadyToInvalidate(SipSessionEvent sipsessionevent) {

  }

}

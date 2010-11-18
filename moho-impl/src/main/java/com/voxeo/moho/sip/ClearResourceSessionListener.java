package com.voxeo.moho.sip;

import javax.servlet.sip.SipSessionEvent;
import javax.servlet.sip.SipSessionListener;
import javax.servlet.sip.annotation.SipListener;

import org.apache.log4j.Logger;

import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.util.SessionUtils;

@SipListener
public class ClearResourceSessionListener implements SipSessionListener {
  private static final Logger LOG = Logger.getLogger(ClearResourceSessionListener.class);

  @Override
  public void sessionCreated(SipSessionEvent sipsessionevent) {

  }

  @Override
  public void sessionDestroyed(SipSessionEvent sipsessionevent) {
    final EventSource source = SessionUtils.getEventSource(sipsessionevent.getSession());

    if (source != null && source instanceof SIPCallImpl) {
      try {
        ((SIPCallImpl) source).disconnect(true);
      }
      catch (Exception ex) {
        // ignore this exception.
        LOG.debug("Moho clearing resource exception:" + ex.getMessage());
      }
    }
  }

  @Override
  public void sessionReadyToInvalidate(SipSessionEvent sipsessionevent) {

  }

}

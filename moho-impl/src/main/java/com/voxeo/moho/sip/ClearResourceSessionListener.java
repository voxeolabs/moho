package com.voxeo.moho.sip;

import javax.servlet.sip.SipSessionEvent;
import javax.servlet.sip.SipSessionListener;
import javax.servlet.sip.annotation.SipListener;

import com.voxeo.moho.ApplicationContextImpl;
import com.voxeo.moho.common.event.MohoCallCompleteEvent;
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
      ((ApplicationContextImpl)((SIPCallImpl) source).getApplicationContext()).getExecutor().execute(new Runnable() {
        @Override
        public void run() {
          ((SIPCallImpl) source).disconnect(true, MohoCallCompleteEvent.Cause.ERROR, null, null);
        }
      });
    }
  }

  @Override
  public void sessionReadyToInvalidate(SipSessionEvent sipsessionevent) {

  }

}

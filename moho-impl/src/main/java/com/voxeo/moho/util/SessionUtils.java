/**
 * Copyright 2010 Voxeo Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.voxeo.moho.util;

import javax.servlet.ServletContext;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipSession;

import org.apache.log4j.Logger;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Participant;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.spi.ExecutionContext;

public class SessionUtils {
  private static final Logger LOG = Logger.getLogger(SessionUtils.class);

  private static final String SESSION_EVENTSOURCE = "session.event.source";

  public static Application getApplication(final SipSession session) {
    return getContext(session).getApplication();
  }

  public static ExecutionContext getContext(final ServletContext ctx) {
    return (ExecutionContext) ctx.getAttribute(ApplicationContext.APPLICATION_CONTEXT);
  }

  public static ExecutionContext getContext(final SipSession session) {
    return getContext(session.getServletContext());
  }

  public static ExecutionContext getContext(final SipServletMessage message) {
    return getContext(message.getSession());
  }

  public static Participant getParticipant(final SipSession session) {
    final EventSource source = getEventSource(session);
    if (source instanceof Participant) {
      return (Participant) source;
    }
    else {
      return null;
    }
  }

  public static Participant getParticipant(final SipServletMessage message) {
    return getParticipant(message.getSession());
  }

  public static EventSource getEventSource(final SipSession session) {
    if(session.isValid()){
      return (EventSource) session.getAttribute(SESSION_EVENTSOURCE);
    }
    else{
      LOG.warn("Session already invalidated, can't get event source. " + session);
      return null;
    }
  }

  public static EventSource getEventSource(final SipServletMessage message) {
    return getEventSource(message.getSession());
  }

  public static void setEventSource(final SipSession session, final EventSource source) {
    session.setAttribute(SESSION_EVENTSOURCE, source);
  }

  public static void removeEventSource(final SipSession session) {
    session.removeAttribute(SESSION_EVENTSOURCE);
  }

}

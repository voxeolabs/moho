/**
 * Copyright 2010-2011 Voxeo Corporation Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.voxeo.moho.event;

import java.util.Map;

import javax.servlet.sip.SipServletResponse;

import com.voxeo.moho.SignalException;

/**
 * This interface marks an event which can be accepted or rejected by the
 * application. An AcceptableEvent will be automatically accepted if no action
 * is taken by the application.
 */
public interface AcceptableEvent {
  public enum Reason {
    BUSY {
      @Override
      public int getCode() {
        return SipServletResponse.SC_BUSY_HERE;
      }
    },

    BUSY_EVERYWHERE {
      @Override
      public int getCode() {
        return SipServletResponse.SC_BUSY_EVERYWHERE;
      }
    },

    TEMPORARLY_UNAVAILABLE {
      @Override
      public int getCode() {
        return SipServletResponse.SC_TEMPORARLY_UNAVAILABLE;
      }
    },

    DECLINE {
      @Override
      public int getCode() {
        return SipServletResponse.SC_DECLINE;
      }
    },

    FORBIDEN {
      @Override
      public int getCode() {
        return SipServletResponse.SC_FORBIDDEN;
      }
    },

    INTERVAL_TOO_BRIEF {
      @Override
      public int getCode() {
        return SipServletResponse.SC_INTERVAL_TOO_BRIEF;
      }
    },

    CONDITIONAL_REQUEST_FAILED {
      @Override
      public int getCode() {
        return SipServletResponse.SC_CONDITIONAL_REQUEST_FAILED;
      }
    },

    BAD_REQUEST {
      @Override
      public int getCode() {
        return SipServletResponse.SC_BAD_REQUEST;
      }
    },

    BAD_EVENT {
      @Override
      public int getCode() {
        return SipServletResponse.SC_BAD_EVENT;
      }
    },

    TIMEOUT {
      @Override
      public int getCode() {
        return SipServletResponse.SC_REQUEST_TIMEOUT;
      }
    },
    
    NOT_FOUND {
      @Override
      public int getCode() {
        return SipServletResponse.SC_NOT_FOUND;
      }
    },
    
    ERROR {
      @Override
      public int getCode() {
        return SipServletResponse.SC_SERVER_INTERNAL_ERROR;
      }
    };

    public abstract int getCode();
  }

  /**
   * @return true if this event has been accepted.
   */
  boolean isAccepted();

  /**
   * @return true if this event has been rejected.
   */
  boolean isRejected();

  /**
   * When the event is accepted, Moho sends positive response to the event based
   * on underlying signaling protocol e.g. 200 OK in SIP.
   * 
   * @throws SignalException
   *           when any signaling error occurs.
   */
  void accept() throws SignalException;

  /**
   * /** When the event is accepted with additional header, Moho sends positive
   * response based on underlying signaling protocol e.g. 200 OK in SIP, with
   * additional protocol specific headers.
   * 
   * @param headers
   *          additional signaling protocol specific headers to be sent with the
   *          response.
   * @throws SignalException
   *           when any signaling error occurs.
   */
  void accept(final Map<String, String> headers) throws SignalException;

  /**
   * When the event is rejected, Moho sends negative response based on the
   * underlying signaling protocol.
   * 
   * @param reason
   *          the reason to reject the event.
   * @throws SignalException
   *           when any signaling error occurs.
   */
  void reject(Reason reason) throws SignalException;

  /**
   * When the event is rejected, Moho sends negative response based on the
   * underlying signaling protocol, with additional protocol specific headers.
   * 
   * @param reason
   *          the reason to reject the event.
   * @param headers
   *          additional signaling protocol specific headers to be sent with the
   *          response.
   * @throws SignalException
   *           when any signaling error occurs.
   */
  void reject(Reason reason, Map<String, String> headers) throws SignalException;

  void setAsync(boolean async);

  boolean isAsync();
}

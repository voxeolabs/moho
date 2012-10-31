/**
 * Copyright 2010-2011 Voxeo Corporation
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

package com.voxeo.moho.sip;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipSession;

import com.voxeo.moho.Call;

/**
 * This interface encapsulates the SIP specific properties of a {@link com.voxoe.moho.Call Call}.
 * 
 * @author wchen
 *
 */
public interface SIPCall extends Call {

  /**
   * SIP specific call states.
   * 
   * @author wchen
   *
   */
  public enum State {
    INITIALIZED, INVITING, PROGRESSING, PROGRESSED, RINGING, ANSWERING, ANSWERED, DISCONNECTED, FAILED, REJECTED, REDIRECTED, PROXIED
  }

  /**
   * @return the underlying JSR 289 SIP session.
   */
  SipSession getSipSession();

  /**
   * @return the SIP specific call state of the current SIPCall.
   */
  SIPCall.State getSIPCallState();

  /**
   * @return the initial SIP INVITE of this call.
   */
  SipServletRequest getSipRequest();
  
  void setContinueRouting(final SIPCall origCall);
}

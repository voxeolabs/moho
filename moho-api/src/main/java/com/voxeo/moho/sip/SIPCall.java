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

package com.voxeo.moho.sip;

import java.util.ListIterator;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipSession;

import com.voxeo.moho.Call;

public abstract class SIPCall extends Call {

  public enum State {
    INITIALIZED, INVITING, PROGRESSING, PROGRESSED, RINGING, ANSWERING, ANSWERED, DISCONNECTED, FAILED,
  }

  public abstract SipSession getSipSession();

  public abstract SIPCall.State getSIPCallState();

  //
  public abstract SipServletRequest getSipRequest();

  public abstract String getHeader(String name);

  public abstract ListIterator<String> getHeaders(String name);

}

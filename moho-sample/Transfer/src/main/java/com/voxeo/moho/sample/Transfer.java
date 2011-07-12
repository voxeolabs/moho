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

package com.voxeo.moho.sample;

import javax.media.mscontrol.join.Joinable;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.State;
import com.voxeo.moho.event.MohoReferEvent;

/**
 * Transfer: support unattended transfer and attended transfer. for unattended
 * transfer: when accepting the ReferEvent, Moho consume REFER request, use 3PCC
 * signaling. for attended transfer: when accepting the ReferEvent, Moho just
 * forward the refer request to the peer. Note that if the application doesn't
 * take any action, such as accept, forward, reject, on the event, Moho will
 * accept it.
 */
public class Transfer implements Application {

  @Override
  public void destroy() {

  }

  @Override
  public void init(final ApplicationContext ctx) {

  }

  @State
  public void handleInvite(final IncomingCall call) {
    call.addObserver(this);
    call.accept();
    final Call outgoingCall = call.getInvitee().call(call.getInvitor());
    outgoingCall.addObserver(this);
    call.join(outgoingCall, JoinType.DIRECT, Joinable.Direction.DUPLEX);
    outgoingCall.setApplicationState("wait");
  }

  @State("wait")
  public void transfer(final MohoReferEvent ev) throws SignalException {
    // you can do your own things here. if you need not do this, you can just
    // remove this method, so you need not set the call in 'supervised' mode in
    // the above handleInvite method.
  }

}

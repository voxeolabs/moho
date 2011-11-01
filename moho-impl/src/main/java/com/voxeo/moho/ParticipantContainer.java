/**
 * Copyright 2010 Voxeo Corporation Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.voxeo.moho;

import java.io.IOException;

import javax.media.mscontrol.join.Joinable.Direction;

import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.common.event.MohoUnjoinCompleteEvent;
import com.voxeo.moho.sip.JoinDelegate;

/**
 * used in internal
 */
public interface ParticipantContainer {

  void addParticipant(Participant p, JoinType type, Direction direction, Participant realJoined);

  MohoUnjoinCompleteEvent doUnjoin(Participant other, boolean callPeerUnjoin) throws Exception;

  void startJoin(Participant participant, JoinDelegate delegate);

  void joinDone(Participant participant, JoinDelegate delegate);

  public JoinDelegate getJoinDelegate(String participantID);

  Direction getDirection(Participant participant);

  // ///the following method is used for direct remote join.
  // return sdp async(by joinDelegate.doInviteReponse) or sync(not answered
  // incoming call)
  byte[] getJoinSDP() throws IOException;

  void processSDPAnswer(byte[] sdp) throws IOException;

  // return sdp async(by joinDelegate.doInviteReponse) or sync(not answered
  // incoming call)
  byte[] processSDPOffer(byte[] sdp) throws IOException;
}

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

package com.voxeo.moho;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;

import javax.media.mscontrol.join.Joinable.Direction;

/**
 * <p>
 * A call is a leg of communication from an {@link Endpoint Endpoint} to the Moho application.
 * The leg must have signal controlled by the Moho application, optionally media
 * as well.
 * </p>
 * <p>
 * A call is an {@link com.voxeo.moho.event.EventSource EventSource} that
 * generates both {@link com.voxeo.moho.event.CallEvent CallEvent} and
 * {@link com.voxeo.moho.event.MohoMediaEvent MediaEvent}. However, all the events
 * from a call are dispatched in a single thread to simplify application
 * programming.
 * </p>
 * 
 * @author wchen
 */
public interface Call extends MultiStreamParticipant, MediaService<Call> {
  public enum State {
    /** the Call object is initialized **/
    INITIALIZED,

    /** call is accepted with early media */
    INPROGRESS,

    /** call is accepted without early media */
    ACCEPTED,

    /** call is connected */
    CONNECTED,

    /** call is disconnected */
    DISCONNECTED,

    /** call is failed */
    FAILED
  }

  /**
   * @return the address that sends the invitation
   */
  Endpoint getInvitor();

  /**
   * @return the address that is supposed to receive invitation
   */
  CallableEndpoint getInvitee();

  /**
   * join the call to media server in
   * {@link javax.media.mscontrol.join.Joinable.Direction.DUPLEX}.
   */
  Joint join();

  /**
   * join the call to media server in the specified direction.
   */
  Joint join(Direction direction);

  /**
   * Connect this participant to the specified endpoint. The signaling protocol
   * used is based on the endpoint type
   * 
   * @param other
   *          the endpoint
   * @param type
   *          whether the media is bridged or direct between the two
   *          participants
   * @param direction
   *          whether the media is full duplex or half-duplex between the two
   *          participants
   * @return the participant of the specified endpoint
   */
  Joint join(CallableEndpoint other, Participant.JoinType type, Direction direction);

  /**
   * Connect this participant to the specified endpoint. The signaling protocol
   * used is based on the endpoint type
   * 
   * @param other
   *          the endpoint
   * @param type
   *          whether the media is bridged or direct between the two
   *          participants
   * @param direction
   *          whether the media is full duplex or half-duplex between the two
   *          participants
   * @param headers
   *          the additional protocol specific headers sent to the endpoint.
   * @return the participant of the specified endpoint
   */
  Joint join(CallableEndpoint other, Participant.JoinType type, Direction direction, Map<String, String> headers);

  /**
   * @return the current signaling state of the call
   */
  State getCallState();

  /**
   * @return the peer participant (from call control point of view)
   */
  Call[] getPeers();

  /**
   * mute the endpoint, make it listen-only.
   */
  void mute();

  /**
   * unmute this call
   */
  void unmute();

  /**
   * hold this call
   */
  void hold();

  /**
   * send a sendrecv SDP and resume to send media data.
   */
  void unhold();
  
  /**
   * disconnect this call.
   */
  void hangup();

  /**
   * disconnect this call with headers.
   */
  void hangup(Map<String, String> headers);
  
  public abstract boolean isHold();
  
  public abstract boolean isMute();
  /**
   * @param name the name of the protocol specific header of the initial call setup message.
   * @return the value of the named header in this invitation. The first value if there are multiple values.
   */
  String getHeader(String name);
  /**
   * @param name the name of the protocol specific header of the initial call setup message.
   * @return iterator for all the values of the named header.
   */
  ListIterator<String> getHeaders(String name);
  
  /**
   * @return iterator for all the names of the protocol specific headers in the initial call setup message.
   */
  Iterator<String> getHeaderNames();
}

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

package com.voxeo.moho;

import java.util.Map;

import javax.media.mscontrol.join.Joinable.Direction;

import com.voxeo.moho.event.InviteEvent;

/**
 * <p>
 * A call is a leg of communication from an Endpoint to the Moho application.
 * The leg must have signal controlled by the Moho application, optionally media
 * as well.
 * </p>
 * <p>
 * A call is an {@link com.voxeo.moho.event.EventSource EventSource} that
 * generates both {@link com.voxeo.moho.event.SignalEvent SignalEvent} and
 * {@link com.voxeo.moho.event.MediaEvent MediaEvent}. However, all the events
 * from a call are dispatched in a single thread to simplify application
 * programming.
 * </p>
 * 
 * @author wchen
 */
public abstract class Call extends InviteEvent implements MultiStreamParticipant {
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

  protected Call() {
    super();
  }

  /**
   * join the call to media server in
   * {@link javax.media.mscontrol.join.Joinable.Direction.DUPLEX}.
   * 
   * @throws IllegalStateException
   *           if the call has been disconnected.
   */
  public abstract Joint join();

  /**
   * join the call to media server in the specified direction.
   * 
   * @throws IllegalStateException
   *           if the call has been disconnected.
   */
  public abstract Joint join(Direction direction);

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
   * @throws IllegalStateException
   *           if the call has been released.
   */
  public abstract Joint join(CallableEndpoint other, Participant.JoinType type, Direction direction);

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
   * @throws IllegalStateException
   *           if the call has been released.
   */
  public abstract Joint join(CallableEndpoint other, Participant.JoinType type, Direction direction,
      Map<String, String> headers);

  /**
   * supervised mode delivers more events to the listener
   * 
   * @return whether this call is supervised or not.
   */
  public abstract boolean isSupervised();

  /**
   * @param supervised
   *          true if to turn on supervised mode
   */
  public abstract void setSupervised(boolean supervised);

  /**
   * return the media service attached to the call
   * 
   * @param reinvite
   *          whether Moho Framework should automatically re-invites the call to
   *          {@link Participant.JoinType#BRIDGE Bridge} mode if the call is
   *          currently joined in {@link Participant.JoinType#DIRECT Direct}
   *          mode.
   * @throws MediaException
   *           when there is media server error.
   * @throws IllegalStateException
   *           when the call is {@link Participant.JoinType#DIRECT Direct} mode
   *           but reinvite is false or if the call is not answered.
   */
  public abstract MediaService getMediaService(boolean reinvite);

  /**
   * return the media service attached to the call. Equivalent of
   * {@link #getMediaService(boolean) getMediaService(true)}.
   * 
   * @throws MediaException
   *           when there is media server error.
   * @throws IllegalStateException
   *           when the call is {@link Participant.JoinType#DIRECT Direct} mode
   *           but reinvite is false or if the call is not answered.
   */
  public abstract MediaService getMediaService();

  /**
   * @return the current signaling state of the call
   */
  public abstract State getCallState();

  /**
   * @return the peer participant (from call control point of view)
   */
  public abstract Call[] getPeers();

  /**
   * mute the endpoint, make it listen-only.
   */
  public abstract void mute();

  /**
   * unmute the endpoint
   */
  public abstract void unmute();

  /**
   * hold the endpoint
   */
  public abstract void hold();

  /**
   * send a sendrecv SDP and resume to send media data.
   */
  public abstract void unhold();
}

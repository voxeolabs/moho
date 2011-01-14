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

import com.voxeo.moho.event.EventState;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.event.RejectableEvent;
import com.voxeo.moho.event.SignalEvent;
import com.voxeo.moho.event.InviteEvent.InviteEventState;
import com.voxeo.utils.EventListener;

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
public abstract class Call extends SignalEvent implements MultiStreamParticipant, RejectableEvent {
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
   * States of the InviteEvent
   * 
   * @author wchen
   */
  public enum InviteState implements EventState {
    /**
     * the initial state of InviteEvent.
     */
    ALERTING,
    /**
     * when one of the accept method is called.
     */
    ACCEPTING,
    /**
     * when one of acceptWithEarlyMedia method is called.
     */
    PROGRESSING,
    /**
     * when one of the reject method is called.
     */
    REJECTING,
    /**
     * when one of the redirect method is called.
     */
    REDIRECTING
  }

  protected Call() {
    super(null);
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

  // invite
  /**
   * @return the address that sends the invitation
   */
  public abstract Endpoint getInvitor();

  /**
   * @return the address that is supposed to receive invitation
   */
  public abstract CallableEndpoint getInvitee();

  /**
   * Accept the event.
   * 
   * @return the {@link Call Call} resulted by accepting the invitation.
   * @throws SignalException
   *           when there is any signal error.
   * @throws IllegalStateException
   *           when the event has been accpeted.
   */
  public Call acceptCall() {
    return this.acceptCall((Observer) null);
  }

  /**
   * Accept the event.
   * 
   * @return the {@link Call Call} resulted by accepting the invitation.
   * @throws SignalException
   *           when there is any signal error.
   * @throws IllegalStateException
   *           when the event has been accpeted.
   */
  public Call acceptCall(final Map<String, String> headers) {
    return this.acceptCall((Map<String, String>) null, (Observer) null);
  }

  /**
   * Accept the invitation with a set of {@link Observer Observer}s.
   * 
   * @param observers
   *          the {@link Observer Observer}s to be added to the {@link Call
   *          Call}
   * @return the {@link Call Call} resulted by accepting the invitation.
   * @throws SignalException
   *           when there is any signal error.
   * @throws IllegalStateException
   *           when the event has been accpeted.
   */
  public Call acceptCall(final Observer... observers) throws SignalException, IllegalStateException {
    return this.acceptCall(null, observers);
  }

  /**
   * Accept the invitation with a set of {@link com.voxeo.utils.EventListener
   * EventListener}s.
   * 
   * @param listeners
   *          the {@link com.voxeo.utils.EventListener EventListener}s to be
   *          added to the {@link Call Call}
   * @return the {@link Call Call} resulted by accepting the invitation.
   * @throws SignalException
   *           when there is any signal error.
   * @throws IllegalStateException
   *           when the event has been accpeted.
   */
  public Call acceptCall(final EventListener<?>... listeners) throws SignalException, IllegalStateException {
    return this.acceptCall(null, listeners);
  }

  /**
   * Accept the invitation with a set of {@link com.voxeo.utils.EventListener
   * EventListener}s and additional headers.
   * 
   * @param headers
   *          additional signaling protocol specific headers to be sent with the
   *          response.
   * @param listeners
   *          the {@link com.voxeo.utils.EventListener EventListener}s to be
   *          added to the {@link Call Call}
   * @return the {@link Call Call} resulted by accepting the invitation.
   * @throws SignalException
   *           when there is any signal error.
   * @throws IllegalStateException
   *           when the event has been accpeted.
   */
  public abstract Call acceptCall(final Map<String, String> headers, final EventListener<?>... listeners)
      throws SignalException, IllegalStateException;

  /**
   * Accept the invitation with a set of {@link Observer Observer}s and
   * additional headers.
   * 
   * @param headers
   *          additional signaling protocol specific headers to be sent with the
   *          response.
   * @param observer
   *          the {@link Observer Observer}s s to be added to the {@link Call
   *          Call}
   * @return the {@link Call Call} resulted by accepting the invitation.
   * @throws SignalException
   *           when there is any signal error.
   * @throws IllegalStateException
   *           when the event has been accpeted.
   */
  public abstract Call acceptCall(final Map<String, String> headers, final Observer... observer)
      throws SignalException, IllegalStateException;

  /**
   * accept the invitation with early media (SIP 183)
   * 
   * @throws SignalException
   *           when there is any signal error.
   * @throws MediaException
   *           when there is any media server error.
   * @throws IllegalStateException
   *           when the event has been accpeted.
   */
  public Call acceptCallWithEarlyMedia() throws SignalException, MediaException, IllegalStateException {
    return this.acceptCallWithEarlyMedia((Map<String, String>) null);
  }

  /**
   * accept the invitation with early media (SIP 183)
   * 
   * @param headers
   *          additional signaling protocol specific headers to be sent with the
   *          response.
   * @return the {@link Call Call} resulted by accepting the invitation.
   * @throws SignalException
   *           when there is any signal error.
   * @throws MediaException
   *           when there is any media server error.
   * @throws IllegalStateException
   *           when the event has been accpeted.
   */
  public Call acceptCallWithEarlyMedia(final Map<String, String> headers) throws SignalException, MediaException,
      IllegalStateException {
    return this.acceptCallWithEarlyMedia(headers, (Observer) null);
  }

  /**
   * accept the invitation with early media (SIP 183)
   * 
   * @param observers
   *          the {@link Observer Observer}s s to be added to the {@link Call
   *          Call}
   * @return the {@link Call Call} resulted by accepting the invitation.
   * @throws SignalException
   *           when there is any signal error.
   * @throws MediaException
   *           when there is any media server error.
   * @throws IllegalStateException
   *           when the event has been accpeted.
   */
  public Call acceptCallWithEarlyMedia(final Observer... observers) throws SignalException, MediaException,
      IllegalStateException {
    return this.acceptCallWithEarlyMedia(null, observers);
  }

  /**
   * accept the invitation with early media (SIP 183) with additional headers
   * 
   * @param listeners
   *          the {@link com.voxeo.utils.EventListener EventListener}s to be
   *          added to the {@link Call Call}
   * @return the {@link Call Call} resulted by accepting the invitation.
   * @throws SignalException
   *           when there is any signal error.
   * @throws MediaException
   *           when there is any media server error.
   * @throws IllegalStateException
   *           when the event has been accpeted.
   */
  public Call acceptCallWithEarlyMedia(final EventListener<?>... listeners) throws SignalException, MediaException,
      IllegalStateException {
    return this.acceptCallWithEarlyMedia(null, listeners);
  }

  /**
   * accept the invitation with early media (SIP 183)
   * 
   * @param headers
   *          additional signaling protocol specific headers to be sent with the
   *          response.
   * @param listeners
   *          the {@link com.voxeo.utils.EventListener EventListener}s to be
   *          added to the {@link Call Call}
   * @return the {@link Call Call} resulted by accepting the invitation.
   * @throws SignalException
   *           when there is any signal error.
   * @throws MediaException
   *           when there is any media server error.
   * @throws IllegalStateException
   *           when the event has been accpeted.
   */
  public abstract Call acceptCallWithEarlyMedia(final Map<String, String> headers, final EventListener<?>... listeners)
      throws SignalException, MediaException, IllegalStateException;

  /**
   * accept the invitation with early media (SIP 183)
   * 
   * @param headers
   *          additional signaling protocol specific headers to be sent with the
   *          response.
   * @param observers
   *          the {@link Observer Observer}s s to be added to the {@link Call
   *          Call}
   * @return the {@link Call Call} resulted by accepting the invitation.
   * @throws SignalException
   *           when there is any signal error.
   * @throws MediaException
   *           when there is any media server error.
   * @throws IllegalStateException
   *           when the event has been accpeted.
   */
  public abstract Call acceptCallWithEarlyMedia(final Map<String, String> headers, final Observer... observers)
      throws SignalException, MediaException, IllegalStateException;

  /**
   * redirect the INVITE to others via 302
   * 
   * @param other
   *          the other endpoint
   * @throws SignalException
   *           when there is any signal error.
   */
  public void redirect(final Endpoint other) throws SignalException, IllegalArgumentException {
    this.redirect(other, null);
  }

  public abstract void redirect(Endpoint other, Map<String, String> headers) throws SignalException,
      IllegalArgumentException;

  /**
   * reject the invitation with reason
   * 
   * @param reason
   * @throws SignalException
   *           when there is any signal error.
   */
  public void reject(final Reason reason) throws SignalException {
    this.reject(reason, null);
  }

  /**
   * Accept the call and join the call to media server.
   * 
   * @return the {@link Call Call} resulted by accepting the invitation.
   * @throws SignalException
   *           when there is any signal error.
   * @throws IllegalStateException
   *           when the event has been accpeted.
   */
  public Call answer() {
    return this.answer((Observer) null);
  }

  /**
   * Accept the call and join the call to media server.
   * 
   * @return the {@link Call Call} resulted by accepting the invitation.
   * @throws SignalException
   *           when there is any signal error.
   * @throws IllegalStateException
   *           when the event has been accpeted.
   */
  public Call answer(final Map<String, String> headers) {
    return this.acceptCall((Map<String, String>) null, (Observer) null);
  }

  /**
   * Accept the invitation with a set of {@link Observer Observer}s and join the
   * call to media server.
   * 
   * @param observers
   *          the {@link Observer Observer}s to be added to the {@link Call
   *          Call}
   * @return the {@link Call Call} resulted by accepting the invitation.
   * @throws SignalException
   *           when there is any signal error.
   * @throws IllegalStateException
   *           when the event has been accpeted.
   */
  public Call answer(final Observer... observers) throws SignalException, IllegalStateException {
    return this.answer(null, observers);
  }

  /**
   * Accept the invitation with a set of {@link com.voxeo.utils.EventListener
   * EventListener}s and join the call to media server.
   * 
   * @param listeners
   *          the {@link com.voxeo.utils.EventListener EventListener}s to be
   *          added to the {@link Call Call}
   * @return the {@link Call Call} resulted by accepting the invitation.
   * @throws SignalException
   *           when there is any signal error.
   * @throws IllegalStateException
   *           when the event has been accpeted.
   */
  public Call answer(final EventListener<?>... listeners) throws SignalException, IllegalStateException {
    return this.answer(null, listeners);
  }

  /**
   * Accept the invitation with a set of {@link com.voxeo.utils.EventListener
   * EventListener}s and additional headers and join the call to media server.
   * 
   * @param headers
   *          additional signaling protocol specific headers to be sent with the
   *          response.
   * @param listeners
   *          the {@link com.voxeo.utils.EventListener EventListener}s to be
   *          added to the {@link Call Call}
   * @return the {@link Call Call} resulted by accepting the invitation.
   * @throws SignalException
   *           when there is any signal error.
   * @throws IllegalStateException
   *           when the event has been accpeted.
   */
  public abstract Call answer(final Map<String, String> headers, final EventListener<?>... listeners)
      throws SignalException, IllegalStateException;

  /**
   * Accept the invitation with a set of {@link Observer Observer}s and
   * additional headers and join the call to media server.
   * 
   * @param headers
   *          additional signaling protocol specific headers to be sent with the
   *          response.
   * @param observer
   *          the {@link Observer Observer}s s to be added to the {@link Call
   *          Call}
   * @return the {@link Call Call} resulted by accepting the invitation.
   * @throws SignalException
   *           when there is any signal error.
   * @throws IllegalStateException
   *           when the event has been accpeted.
   */
  public abstract Call answer(final Map<String, String> headers, final Observer... observer) throws SignalException,
      IllegalStateException;

  @Override
  protected synchronized void checkState() {
    if (getState() != InviteEventState.ALERTING) {
      throw new IllegalStateException("Event already " + getState());
    }
  }
}

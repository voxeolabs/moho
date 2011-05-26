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

package com.voxeo.moho.event;

import java.util.Map;

import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.MediaException;
import com.voxeo.moho.SignalException;

/**
 * Invitation is an incoming call alert. This is a key event to start the call
 * control.
 * 
 * @author wchen
 */
public abstract class InviteEvent extends SignalEvent implements RejectableEvent, RedirectableEvent {

  private static final long serialVersionUID = 8264519475273617594L;

  protected boolean _acceptedWithEarlyMedia = false;

  protected boolean _rejected = false;

  protected boolean _redirected = false;

  protected InviteEvent() {
    super(null);
  }

  /**
   * @return the address that sends the invitation
   */
  public abstract Endpoint getInvitor();

  /**
   * @return the address that is supposed to receive invitation
   */
  public abstract CallableEndpoint getInvitee();

  public boolean isAcceptedWithEarlyMedia() {
    return _acceptedWithEarlyMedia;
  }

  @Override
  public boolean isRedirected() {
    return _redirected;
  }

  @Override
  public boolean isRejected() {
    return _rejected;
  }

  @Override
  public boolean isProcessed() {
    return isAccepted() || isAcceptedWithEarlyMedia() || isRejected() || isRedirected();
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
    return this.answer((Map<String, String>) null, (Observer) null);
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

}

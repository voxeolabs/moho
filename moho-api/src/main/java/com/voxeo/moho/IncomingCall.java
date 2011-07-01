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

import java.util.Map;

import com.voxeo.moho.event.InviteEvent;
import com.voxeo.moho.event.Observer;

/**
 * This event is fired when there is any incoming {@link com.voxeo.moho.Call Call}.
 * 
 * @author wchen
 */
public interface IncomingCall extends Call, InviteEvent {

  /**
   * @return true if this incoming call is accepted with early media
   */
  boolean isAcceptedWithEarlyMedia();

  /**
   * accept this incoming call with early media (SIP 183)
   * 
   * @throws SignalException
   *           when there is any signal error.
   * @throws MediaException
   *           when there is any media server error.
   */
  void acceptWithEarlyMedia() throws SignalException, MediaException;

  /**
   * accept this incoming call with early media (SIP 183)
   * 
   * @throws SignalException
   *           when there is any signal error.
   * @throws MediaException
   *           when there is any media server error.
   */
  void acceptWithEarlyMedia(Observer... observer) throws SignalException, MediaException;
  
  /**
   * accept this incoming with early media (SIP 183) and protocol specific headers.
   * 
   * @param headers additional signaling protocol specific headers to be sent with the early media response.
   * @throws SignalException when signaling error occurs during call setups.
   * @throws MediaException when media error occurs during early media negotiation.
   */
  void acceptWithEarlyMedia(final Map<String, String> headers) throws SignalException, MediaException;

  /**
   * accept this incoming call with {@link com.voxeo.event.Observer Observer}s added to the call.
   * 
   * @param observer
   * @throws SignalException
   */
  void accept(Observer... observer) throws SignalException;
  
  /**
   * <p>Accept the call and join the call to media server, as the following</p>
   * <code><pre>  event.accept();
   *   event.join().get();
   * </pre></code>
   * Please note this is a synchronized operation -- 
   * operation doesn't return until the call is completely joined to the media server,
   * or an exception is thrown.
   * 
   * @throws SignalException when signaling error occurs during call setups.
   * @throws MediaException when media error occurs during media negotiation.
   */
  void answer() throws SignalException, MediaException;
  
  /**
   * answer the call with {@link com.voxeo.event.Observer Observer}s added to the call.
   * @param observer
   * @throws SignalException
   * @throws MediaException
   * @see {@link IncomingCall#answer() answer()}
   */
  void answer(Observer... observer) throws SignalException, MediaException;

  /**
   * answer the call with additional protocol specific headers.
   *  
   * @param headers the protocol specific headers to be sent with the response.
   * @throws SignalException when signaling error occurs during call setups.
   * @throws MediaException when media error occurs during media negotiation.
   * @see {@link IncomingCall#answer() answer()}
   */
  void answer(final Map<String, String> headers) throws SignalException, MediaException;
  
  void proxyTo(boolean recordRoute, boolean parallel, Endpoint... destinations) throws SignalException;

}

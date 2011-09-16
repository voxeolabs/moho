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

/**
 * An type of Endpoint that be can be called.
 * 
 * @author wchen
 */
public interface CallableEndpoint extends Endpoint {
  /**
   * Create a {@link Call} to this address. The call has yet been made until
   * {@link Call} is joined to the media server or another {@link Endpoint}.
   * 
   * @deprecated use createCall(Endpoint caller);
   * @param caller
   *          the address of the caller
   * @return the {@link Call} this address.
   */
  Call call(Endpoint caller);

  /**
   * Create a {@link Call} to this address. The call has yet been made until
   * {@link Call} is joined to the media server or another {@link Endpoint}.
   * 
   * @deprecated use createCall(Endpoint caller, Map<String, String> headers);
   * @param caller
   *          the address of the caller
   * @param headers
   *          the additional protocol headers to be sent to the caller when the
   *          call is made.
   * @return the {@link Call} this address.
   */
  Call call(Endpoint caller, Map<String, String> headers);

  /**
   * Create a {@link Call} to this address. The call has yet been made until
   * {@link Call} is joined to the media server or another {@link Endpoint}.
   * 
   * @deprecated user createCall(String caller);
   * @param caller
   *          the address of the caller
   * @return the {@link Call} this address.
   */
  Call call(String caller);

  /**
   * Create a {@link Call} to this address. The call has yet been made until
   * {@link Call} is joined to the media server or another {@link Endpoint}.
   * 
   * @deprecated use createCall(String caller, Map<String, String> headers);
   * @param caller
   *          the address of the caller
   * @param headers
   *          the additional protocol headers to be sent to the caller when the
   *          call is made.
   * @return the {@link Call} this address.
   */
  Call call(String caller, Map<String, String> headers);

  /**
   * Create a subscription to this address.
   * 
   * @param subscriber
   *          the address of the subscriber.
   * @param type
   *          the event type of the subscription
   * @param expiration
   *          the expiration time in seconds
   * @return the subscription made to this endpoint
   * @throws SignalException
   *           when there is any signal error.
   */
  // TODO delay the subscription until renew() is called.
  Subscription subscribe(Endpoint subscriber, Subscription.Type type, int expiration);

  /**
   * Create a {@link Call} to this address. The call has yet been made until
   * {@link Call} is joined to the media server or another {@link Endpoint}.
   * 
   * @param caller
   *          the address of the caller
   * @return the {@link Call} this address.
   */
  Call createCall(Endpoint caller);

  /**
   * Create a {@link Call} to this address. The call has yet been made until
   * {@link Call} is joined to the media server or another {@link Endpoint}.
   * 
   * @param caller
   *          the address of the caller
   * @param headers
   *          the additional protocol headers to be sent to the caller when the
   *          call is made.
   * @return the {@link Call} this address.
   */
  Call createCall(Endpoint caller, Map<String, String> headers);

  /**
   * Create a {@link Call} to this address. The call has yet been made until
   * {@link Call} is joined to the media server or another {@link Endpoint}.
   * 
   * @param caller
   *          the address of the caller
   * @return the {@link Call} this address.
   */
  Call createCall(String caller);

  /**
   * Create a {@link Call} to this address. The call has yet been made until
   * {@link Call} is joined to the media server or another {@link Endpoint}.
   * 
   * @param caller
   *          the address of the caller
   * @param headers
   *          the additional protocol headers to be sent to the caller when the
   *          call is made.
   * @return the {@link Call} this address.
   */
  Call createCall(String caller, Map<String, String> headers);
  
}

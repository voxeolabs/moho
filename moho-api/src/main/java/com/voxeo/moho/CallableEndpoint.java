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

import com.voxeo.moho.event.Observer;
import com.voxeo.utils.EventListener;

/**
 * An type of Endpoint that be can be called.
 * 
 * @author wchen
 */
public interface CallableEndpoint extends Endpoint {

  /**
   * Make a call to this address. TODO: what state is the returned Call in?
   * ACCEPTED? CONNECTED? (ACCEPTED)
   * 
   * @param caller
   * @param listener
   * @return the outbound call made to this endpoint
   * @throws SignalException
   *           when there is any signal error.
   * @throws MediaException
   *           when there is any media server error.
   */
  Call call(Endpoint caller, Map<String, String> headers, EventListener<?>... listener);

  /**
   * Make a call to this address.
   * 
   * @param caller
   * @param headers
   * @param observer
   * @return outbound call made to this endpoint
   */
  Call call(Endpoint caller, Map<String, String> headers, Observer... observers);
  
  Call call(String caller);
  
  Call call(Endpoint caller);
  
  Call call(Endpoint caller, Observer... observers);
  
  Call call(String caller, Observer... observers);

  /**
   * Create a subscription to this address
   * 
   * @param caller
   * @param type
   *          the event type of the subscription
   * @param expiration
   *          the expiration time in seconds
   * @param listener
   *          the listener for this subscription
   * @return the subscription made to this endpoint
   * @throws SignalException
   *           when there is any signal error.
   * @throws MediaException
   *           when there is any media server error.
   */
  Subscription subscribe(Endpoint caller, Subscription.Type type, int expiration, EventListener<?>... listener);

  /**
   * Create a subscription to this address
   * 
   * @param caller
   * @param type
   *          the event type of the subscription
   * @param expiration
   *          the expiration time in seconds
   * @param observers
   *          the observers for this subscription
   * @return the subscription made to this endpoint
   * @throws SignalException
   *           when there is any signal error.
   * @throws MediaException
   *           when there is any media server error.
   */
  Subscription subscribe(Endpoint caller, Subscription.Type type, int expiration, Observer... observers);
}

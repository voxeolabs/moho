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

package com.voxeo.moho.event;

import java.util.Map;

import javax.media.mscontrol.join.Joinable.Direction;

import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.Participant.JoinType;

/**
 * This event is fired when a call has been referred to another {@link com.voxeo.moho.Endpoint Endpoint}.
 * 
 * @author wchen
 */
public interface ReferEvent extends CallEvent, AcceptableEvent, ForwardableEvent {

  public enum TransferType {
    BRIDGE, BLIND
  }

  /**
   * @return the referred address.
   */
  CallableEndpoint getReferee();

  /**
   * @return who made the referral.
   */
  CallableEndpoint getReferredBy();
  
  /**
   * Accept the event to make the call to connected to the referred address.
   * @param type whether to connect the call to the referred address with bridged media or direct media. 
   * @param direction whether to connect the call to the referred address with duplex or half-plex media.
   * @param headers additional protocol specific headers to be sent when connecting to the referred address
   * @return
   * @throws SignalException
   */
  Call accept(final JoinType type, final Direction direction, final Map<String, String> headers) throws SignalException;
}

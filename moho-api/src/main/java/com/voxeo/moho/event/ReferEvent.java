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

import javax.media.mscontrol.join.Joinable.Direction;

import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.Participant.JoinType;

/**
 * This represents a SIP REFER transfer request.
 * 
 * @author wchen
 */
public abstract class ReferEvent extends SignalEvent implements ForwardableEvent {

  private static final long serialVersionUID = 4342028709927637088L;

  protected boolean _forwarded = false;

  public enum TransferType {
    BRIDGE, BLIND
  }

  protected ReferEvent(final EventSource source) {
    super(source);
  }

  public abstract CallableEndpoint getReferee();

  public abstract CallableEndpoint getReferredBy();

  public abstract Call accept(final JoinType type, final Direction direction, final Map<String, String> headers)
      throws SignalException;

  @Override
  public boolean isForwarded() {
    return _forwarded;
  }

  @Override
  public boolean isProcessed() {
    return isAccepted() || isForwarded();
  }
}

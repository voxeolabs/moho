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

package com.voxeo.rayo.mohoremote.impl.event;

import java.util.Map;

import javax.media.mscontrol.join.Joinable.Direction;

import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.event.ReferEvent;

public abstract class MohoReferEvent extends MohoCallEvent implements ReferEvent {

  private static final long serialVersionUID = 4342028709927637088L;

  protected boolean _forwarded = false;

  protected boolean _accepted = false;

  protected boolean _rejected = false;

  public enum TransferType {
    BRIDGE, BLIND
  }

  protected MohoReferEvent(final Call source) {
    super(source);
  }

  public abstract CallableEndpoint getReferee();

  public abstract CallableEndpoint getReferredBy();

  @Override
  public boolean isAccepted() {
    return _accepted;
  }

  @Override
  public void accept() throws SignalException {
    accept(JoinType.DIRECT, Direction.DUPLEX, null);
  }

  @Override
  public void accept(final Map<String, String> headers) throws SignalException {
    accept(JoinType.DIRECT, Direction.DUPLEX, headers);
  }

  @Override
  public void forwardTo(final Call call) throws SignalException {
    forwardTo(call, null);
  }

  @Override
  public boolean isRejected() {
    return _rejected;
  }

  @Override
  public void reject(Reason reason) {
    reject(reason, null);
  }

  @Override
  public boolean isForwarded() {
    return _forwarded;
  }

  @Override
  public boolean isProcessed() {
    return isAccepted() || isRejected() || isForwarded();
  }
}

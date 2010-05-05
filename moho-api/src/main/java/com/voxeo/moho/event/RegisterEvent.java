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

import com.voxeo.moho.Endpoint;
import com.voxeo.moho.SignalException;

public abstract class RegisterEvent extends SignalEvent implements RejectableEvent {

  protected RegisterEvent(final EventSource source) {
    super(source);
  }

  public abstract Endpoint getEndpoint();

  public abstract Endpoint[] getContacts();

  public abstract int getExpiration();

  public abstract void accept(Endpoint[] contacts, int expiration, Map<String, String> headres);

  public void accept(final Map<String, String> headers) throws SignalException, IllegalStateException {
    accept(getContacts(), getExpiration(), headers);
  }

  public void reject(final Reason reason) throws SignalException {
    this.reject(reason, null);
  }

}

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

import com.voxeo.moho.Endpoint;
import com.voxeo.moho.MediaException;
import com.voxeo.moho.SignalException;

public abstract class MohoInviteEvent extends MohoCallEvent implements InviteEvent {

  protected boolean _acceptedWithEarlyMedia = false;

  protected boolean _rejected = false;

  protected boolean _redirected = false;
  
  protected boolean _accepted = false;

  protected MohoInviteEvent() {
    super(null);
  }

  @Override
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
  public boolean isAccepted() {
    return _accepted;
  }
  
  @Override
  public boolean isProcessed() {
    return isAccepted() || isAcceptedWithEarlyMedia() || isRejected() || isRedirected();
  }

  @Override
  public void acceptWithEarlyMedia() throws SignalException, MediaException {
    this.acceptWithEarlyMedia((Map<String, String>) null);
  }

  @Override
  public void redirect(final Endpoint other) throws SignalException {
    this.redirect(other, null);
  }

  @Override
  public void reject(final Reason reason) throws SignalException {
    this.reject(reason, null);
  }

  @Override
  public void answer() {
    this.answer((Map<String, String>) null);
  }
}

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

import com.voxeo.moho.Endpoint;
import com.voxeo.moho.Framework;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.event.SubscribeEvent;

public abstract class MohoSubscribeEvent extends MohoEvent<Framework> implements SubscribeEvent {
  protected boolean _rejected = false;

  protected boolean _accepted = false;

  protected boolean _redirected = false;

  protected boolean _proxied = false;

  protected MohoSubscribeEvent(final Framework source) {
    super(source);
  }

  @Override
  public synchronized boolean isAccepted() {
    return _accepted;
  }

  @Override
  public synchronized boolean isRedirected() {
    return _redirected;
  }

  @Override
  public synchronized boolean isProxied() {
    return _proxied;
  }

  @Override
  public void accept() throws SignalException, IllegalStateException {
    accept(null);
  }

  @Override
  public void reject(Reason reason) throws SignalException {
    this.reject(reason, null);
  }

  @Override
  public void redirect(Endpoint other) throws SignalException {
    this.redirect(other, null);
  }

  @Override
  public synchronized boolean isRejected() {
    return _rejected;
  }

  @Override
  public synchronized boolean isProcessed() {
    return isAccepted() || isRejected() || isRedirected() || isProxied();
  }

  protected synchronized void checkState() {
    if (isProcessed()) {
      throw new IllegalStateException("Event is already processed and can not be processed.");
    }
  }
}

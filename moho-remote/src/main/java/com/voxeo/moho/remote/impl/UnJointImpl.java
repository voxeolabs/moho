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

package com.voxeo.moho.remote.impl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.voxeo.moho.SignalException;
import com.voxeo.moho.Unjoint;
import com.voxeo.moho.common.util.SettableResultFuture;
import com.voxeo.moho.event.UnjoinCompleteEvent;

public class UnJointImpl implements Unjoint {
  protected SettableResultFuture<UnjoinCompleteEvent> _future = new SettableResultFuture<UnjoinCompleteEvent>();

  protected CallImpl _call;

  public UnJointImpl(final CallImpl call) {
    _call = call;
  }

  public void done(final UnjoinCompleteEvent event) {
    _future.setResult(event);
  }

  public void done(final SignalException exception) {
    _future.setException(exception);
  }

  @Override
  public boolean cancel(final boolean mayInterruptIfRunning) {
    return _future.cancel(mayInterruptIfRunning);
  }

  @Override
  public UnjoinCompleteEvent get() throws InterruptedException, ExecutionException {
    return _future.get();
  }

  @Override
  public UnjoinCompleteEvent get(final long timeout, final TimeUnit unit) throws InterruptedException,
      ExecutionException, TimeoutException {
    return _future.get(timeout, unit);
  }

  @Override
  public boolean isCancelled() {
    return _future.isCancelled();
  }

  @Override
  public boolean isDone() {
    return _future.isDone();
  }
}

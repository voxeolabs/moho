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

package com.voxeo.rayo.mohoremote.impl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.media.mscontrol.join.Joinable.Direction;

import com.voxeo.moho.Joint;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.SignalException;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.rayo.mohoremote.impl.utils.SettableResultFuture;

public class JointImpl implements Joint {
  protected SettableResultFuture<JoinCompleteEvent> _future = new SettableResultFuture<JoinCompleteEvent>();

  protected CallImpl _call;

  protected JoinType _type;

  protected Direction _direction;

  public JointImpl(final CallImpl call, JoinType type, Direction direction) {
    _call = call;
    _type = type;
    _direction = direction;
  }

  public void done(final JoinCompleteEvent event) {
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
  public JoinCompleteEvent get() throws InterruptedException, ExecutionException {
    return _future.get();
  }

  @Override
  public JoinCompleteEvent get(final long timeout, final TimeUnit unit) throws InterruptedException,
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

  public JoinType getType() {
    return _type;
  }

  public Direction getDirection() {
    return _direction;
  }

}

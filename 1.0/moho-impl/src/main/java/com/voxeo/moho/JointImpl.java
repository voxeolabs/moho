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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.voxeo.moho.event.JoinCompleteEvent;

public class JointImpl implements Joint {

  protected FutureTask<JoinCompleteEvent> _future = null;

  protected JoinWorker _worker = null;

  public JointImpl(final Executor executor, final JoinWorker worker) {
    _worker = worker;
    _future = new FutureTask<JoinCompleteEvent>(worker);
    executor.execute(_future);
  }

  @Override
  public JoinCompleteEvent get() throws InterruptedException, ExecutionException {
    return _future.get();
  }

  @Override
  public JoinCompleteEvent get(final long timeout, final TimeUnit unit) throws InterruptedException,
      ExecutionException, TimeoutException {
    try {
      return _future.get(timeout, unit);
    }
    catch (final TimeoutException e) {
      _worker.cancel();
      throw e;
    }
  }

  @Override
  public boolean cancel(final boolean mayInterruptIfRunning) {
    if (_future.cancel(mayInterruptIfRunning)) {
      return _worker.cancel();
    }
    return false;
  }

  @Override
  public boolean isCancelled() {
    return _future.isCancelled();
  }

  @Override
  public boolean isDone() {
    return _future.isDone();
  }

  public static class DummyJoinWorker implements JoinWorker {

    private Participant _joiner;

    private Participant _joinee;

    private Exception _e;

    public DummyJoinWorker(final Participant joiner, final Participant joinee) {
      this(joiner, joinee, null);
    }

    public DummyJoinWorker(final Participant joiner, final Participant joinee, final Exception e) {
      _joiner = joiner;
      _joinee = joinee;
      _e = e;
    }

    public JoinCompleteEvent call() throws Exception {
      if (_e == null) {
        return new JoinCompleteEvent(_joiner, _joinee);
      }
      else {
        throw _e;
      }
    }

    @Override
    public boolean cancel() {
      return false;
    }
  }

}

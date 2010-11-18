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

package com.voxeo.moho.media;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.mediagroup.signals.SignalDetector;

import com.voxeo.moho.ExecutionContext;
import com.voxeo.moho.event.InputCompleteEvent;

public class InputImpl implements Input {

  protected MediaGroup _group;

  protected FutureTask<InputCompleteEvent> _future = null;

  protected InputCompleteEvent _event = null;

  protected Object _lock = new Object();

  protected ExecutionContext _context;

  protected boolean _startedFuture = false;

  protected InputImpl(final MediaGroup group, ExecutionContext context) {
    _group = group;
    _context = context;
  }

  protected Input prepare() {
    _future = new FutureTask<InputCompleteEvent>(new Callable<InputCompleteEvent>() {
      @Override
      public InputCompleteEvent call() throws Exception {
        synchronized (_lock) {
          while (_event == null) {
            _lock.wait();
          }
        }
        return _event;
      }

    });

    return this;
  }

  protected void done(final InputCompleteEvent event) {
    synchronized (_lock) {
      _event = event;
      _lock.notifyAll();
    }
  }

  @Override
  public void stop() {
    _group.triggerAction(SignalDetector.STOP);
  }

  @Override
  public boolean cancel(final boolean mayInterruptIfRunning) {
    startFuture();
    return _future.cancel(mayInterruptIfRunning);
  }

  @Override
  public InputCompleteEvent get() throws InterruptedException, ExecutionException {
    startFuture();
    return _future.get();
  }

  @Override
  public InputCompleteEvent get(final long timeout, final TimeUnit unit) throws InterruptedException,
      ExecutionException, TimeoutException {
    startFuture();
    return _future.get(timeout, unit);
  }

  @Override
  public boolean isCancelled() {
    startFuture();
    return _future.isCancelled();
  }

  @Override
  public boolean isDone() {
    startFuture();
    return _future.isDone();
  }

  public synchronized boolean isPending() {
    if (!_startedFuture) {
      return false;
    }
    else {
      return !_future.isDone();
    }
  }

  private synchronized void startFuture() {
    if (!_startedFuture) {
      if (_context != null) {
        _context.getExecutor().execute(_future);
      }
      else {
        new Thread(_future).start();
      }
      _startedFuture = true;
    }
  }
}

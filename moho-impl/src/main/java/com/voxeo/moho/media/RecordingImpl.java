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

package com.voxeo.moho.media;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.mediagroup.Recorder;

import com.voxeo.moho.MediaException;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.RecordCompleteEvent;
import com.voxeo.moho.util.SettableResultFuture;

public class RecordingImpl<T extends EventSource> implements Recording<T> {

  protected MediaGroup _group;

  protected SettableResultFuture<RecordCompleteEvent<T>> _future = new SettableResultFuture<RecordCompleteEvent<T>>();

  final Lock lock = new ReentrantLock();

  private Condition pauseActionResult = lock.newCondition();

  private Condition resumeActionResult = lock.newCondition();

  private boolean _normalDisconnected = false;

  protected RecordingImpl(final MediaGroup group) {
    _group = group;
  }

  protected void done(final RecordCompleteEvent<T> event) {
    _future.setResult(event);
  }

  protected void done(final MediaException exception) {
    _future.setException(exception);
  }

  protected boolean paused = false;

  protected boolean pauseResult = false;

  @Override
  public void pause() {
    lock.lock();
    try {
      if (!_future.isDone() && !paused) {
        _group.triggerAction(Recorder.PAUSE);

        while (!pauseResult) {
          try {
            pauseActionResult.await();
          }
          catch (InterruptedException e) {
            // ignore
          }
        }

        pauseResult = false;
      }
    }
    finally {
      lock.unlock();
    }
  }

  protected void pauseActionDone() {
    lock.lock();
    pauseResult = true;
    paused = true;
    try {
      pauseActionResult.signalAll();
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  public void resume() {
    lock.lock();
    try {
      if (!_future.isDone() && paused) {
        _group.triggerAction(Recorder.RESUME);

        while (!resumeResult) {
          try {
            resumeActionResult.await();
          }
          catch (InterruptedException e) {
            // ignore
          }
        }

        resumeResult = false;
      }
    }
    finally {
      lock.unlock();
    }
  }

  protected boolean resumeResult = false;

  protected void resumeActionDone() {
    lock.lock();
    resumeResult = true;
    paused = false;
    try {
      resumeActionResult.signalAll();
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  public void stop() {
    if (!_future.isDone()) {
      _group.triggerAction(Recorder.STOP);

      try {
        _future.get();
      }
      catch (InterruptedException e) {
        // ignore
      }
      catch (ExecutionException e) {
        // ignore
      }
    }
  }

  @Override
  public boolean cancel(final boolean mayInterruptIfRunning) {
    return _future.cancel(mayInterruptIfRunning);
  }

  @Override
  public RecordCompleteEvent<T> get() throws InterruptedException, ExecutionException {
    return _future.get();
  }

  @Override
  public RecordCompleteEvent<T> get(final long timeout, final TimeUnit unit) throws InterruptedException,
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

  public synchronized boolean isPending() {
    return !_future.isDone();
  }

  public void normalDisconnect(boolean normal) {
    _normalDisconnected = true;
  }

  public boolean isNormalDisconnect() {
    return _normalDisconnected;
  }
}

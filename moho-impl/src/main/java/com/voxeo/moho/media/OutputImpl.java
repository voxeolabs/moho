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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.mediagroup.Player;

import com.voxeo.moho.event.OutputCompleteEvent;
import com.voxeo.moho.util.SettableResultFuture;

public class OutputImpl implements Output {

  protected MediaGroup _group;

  protected SettableResultFuture<OutputCompleteEvent> _future = new SettableResultFuture<OutputCompleteEvent>();

  final Lock lock = new ReentrantLock();

  private Condition speedActionResult = lock.newCondition();

  private Condition volumeActionResult = lock.newCondition();

  private Condition pauseActionResult = lock.newCondition();

  private Condition resumeActionResult = lock.newCondition();
  
  private boolean _normalDisconnected = false;

  protected OutputImpl(final MediaGroup group) {
    _group = group;
  }

  protected void done(final OutputCompleteEvent event) {
    _future.setResult(event);
  }

  @Override
  public synchronized void jump(final int index) {
    if (!_future.isDone()) {
      final Parameters params = _group.getParameters(null);
      final int oldValue = (Integer) params.get(Player.JUMP_PLAYLIST_INCREMENT);
      try {
        if (index > 0) {
          params.put(Player.JUMP_PLAYLIST_INCREMENT, index);
          _group.setParameters(params);
          _group.triggerAction(Player.JUMP_FORWARD_IN_PLAYLIST);
        }
        else if (index < 0) {
          params.put(Player.JUMP_PLAYLIST_INCREMENT, -index);
          _group.setParameters(params);
          _group.triggerAction(Player.JUMP_BACKWARD_IN_PLAYLIST);
        }
      }
      finally {
        params.put(Player.JUMP_PLAYLIST_INCREMENT, oldValue);
        _group.setParameters(params);
      }
    }
  }

  @Override
  public synchronized void move(final boolean direction, final int time) {
    if (!_future.isDone()) {
      final Parameters params = _group.getParameters(null);
      final int oldValue = (Integer) params.get(Player.JUMP_TIME);
      params.put(Player.JUMP_TIME, time);
      _group.setParameters(params);
      try {
        if (direction) {
          _group.triggerAction(Player.JUMP_FORWARD);
        }
        else {
          _group.triggerAction(Player.JUMP_BACKWARD);
        }
      }
      finally {
        params.put(Player.JUMP_TIME, oldValue);
        _group.setParameters(params);
      }
    }
  }

  @Override
  public void speed(final boolean upOrDown) {
    lock.lock();
    try {
      if (!_future.isDone()) {
        if (upOrDown) {
          _group.triggerAction(Player.SPEED_UP);
        }
        else {
          _group.triggerAction(Player.SPEED_DOWN);
        }

        while (!speedResult) {
          try {
            speedActionResult.await();
          }
          catch (InterruptedException e) {
            // ignore
          }
        }
        speedResult = false;
      }
    }
    finally {
      lock.unlock();
    }
  }

  protected boolean speedResult = false;

  protected void speedActionDone() {
    lock.lock();
    speedResult = true;
    try {
      speedActionResult.signalAll();
    }
    finally {
      lock.unlock();
    }
  }

  protected boolean volumeResult = false;

  @Override
  public void volume(final boolean upOrDown) {
    lock.lock();
    try {
      if (!_future.isDone()) {
        if (upOrDown) {
          _group.triggerAction(Player.VOLUME_UP);
        }
        else {
          _group.triggerAction(Player.VOLUME_DOWN);
        }

        while (!volumeResult) {
          try {
            volumeActionResult.await();
          }
          catch (InterruptedException e) {
            // ignore
          }
        }
        volumeResult = false;
      }
    }
    finally {
      lock.unlock();
    }
  }

  protected void volumeActionDone() {
    lock.lock();
    volumeResult = true;
    try {
      volumeActionResult.signalAll();
    }
    finally {
      lock.unlock();
    }
  }

  protected boolean paused = false;

  protected boolean pauseResult = false;

  @Override
  public void pause() {
    lock.lock();
    try {
      if (!_future.isDone() && !paused) {
        _group.triggerAction(Player.PAUSE);

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
        _group.triggerAction(Player.RESUME);

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
      _group.triggerAction(Player.STOP);
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
  public OutputCompleteEvent get() throws InterruptedException, ExecutionException {
    return _future.get();
  }

  @Override
  public OutputCompleteEvent get(final long timeout, final TimeUnit unit) throws InterruptedException,
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

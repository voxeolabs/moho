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

import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.mediagroup.Player;

import com.voxeo.moho.event.OutputCompleteEvent;

public class OutputImpl implements Output {

  protected MediaGroup _group;

  protected FutureTask<OutputCompleteEvent> _future = null;

  protected OutputCompleteEvent _event = null;

  protected Object _lock = new Object();

  protected OutputImpl(final MediaGroup group) {
    _group = group;
  }

  protected Output prepare() {
    _future = new FutureTask<OutputCompleteEvent>(new Callable<OutputCompleteEvent>() {
      @Override
      public OutputCompleteEvent call() throws Exception {
        synchronized (_lock) {
          while (_event == null) {
            _lock.wait();
          }
        }
        return _event;
      }

    });
    new Thread(_future).start();
    return this;
  }

  protected void done(final OutputCompleteEvent event) {
    synchronized (_lock) {
      _event = event;
      _lock.notifyAll();
    }
  }

  @Override
  public synchronized void jump(final int index) {
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

  @Override
  public synchronized void move(final boolean direction, final int time) {
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

  @Override
  public void speed(final boolean upOrDown) {
    if (upOrDown) {
      _group.triggerAction(Player.SPEED_UP);
    }
    else {
      _group.triggerAction(Player.SPEED_DOWN);
    }
  }

  @Override
  public void volume(final boolean upOrDown) {
    if (upOrDown) {
      _group.triggerAction(Player.VOLUME_UP);
    }
    else {
      _group.triggerAction(Player.VOLUME_DOWN);
    }
  }

  @Override
  public void pause() {
    _group.triggerAction(Player.PAUSE);
  }

  @Override
  public void resume() {
    _group.triggerAction(Player.RESUME);
  }

  @Override
  public void stop() {
    _group.triggerAction(Player.STOP);
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

}

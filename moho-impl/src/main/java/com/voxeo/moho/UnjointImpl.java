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

package com.voxeo.moho;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.voxeo.moho.common.util.InheritLogContextFutureTask;
import com.voxeo.moho.event.UnjoinCompleteEvent;

public class UnjointImpl implements Unjoint {

  protected FutureTask<UnjoinCompleteEvent> _future = null;

  protected Callable<UnjoinCompleteEvent> _worker = null;

  public UnjointImpl(final Executor executor, final Callable<UnjoinCompleteEvent> worker) {
    _worker = worker;
    _future = new InheritLogContextFutureTask<UnjoinCompleteEvent>(worker);
    executor.execute(_future);
  }

  @Override
  public UnjoinCompleteEvent get() throws InterruptedException, ExecutionException {
    return _future.get();
  }

  @Override
  public UnjoinCompleteEvent get(final long timeout, final TimeUnit unit) throws InterruptedException,
      ExecutionException, TimeoutException {
    try {
      return _future.get(timeout, unit);
    }
    catch (final TimeoutException e) {
      _future.cancel(true);
      throw e;
    }
  }

  @Override
  public boolean cancel(final boolean mayInterruptIfRunning) {
    return _future.cancel(mayInterruptIfRunning);
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

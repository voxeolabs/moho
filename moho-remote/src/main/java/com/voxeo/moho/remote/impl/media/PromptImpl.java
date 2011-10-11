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

package com.voxeo.moho.remote.impl.media;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;

import com.voxeo.moho.MediaException;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.media.Input;
import com.voxeo.moho.media.Output;
import com.voxeo.moho.media.Prompt;

public class PromptImpl<T extends EventSource> implements Prompt<T> {

  protected Output<T> _output;

  protected Input<T> _input;

  protected FutureTask<Input<T>> _future = null;

  protected Executor _executor;

  public PromptImpl(Executor executor) {
    _executor = executor;
  }

  public void setInput(final Input<T> input) {
    _input = input;
  }

  public void inputGetReady(final Callable<Input<T>> call) {
    _future = new FutureTask<Input<T>>(call);
  }

  public void inputGetSet() {
    if (_executor != null) {
      _executor.execute(_future);
    }
    else {
      new Thread(_future).start();
    }
  }

  public void setOutput(final Output<T> output) {
    _output = output;
  }

  @Override
  public Input<T> getInput() throws MediaException {
    if (_future != null) {
      try {
        _input = _future.get();
      }
      catch (final Exception e) {
        throw new MediaException(e);
      }
    }
    return _input;
  }

  @Override
  public Output<T> getOutput() {
    return _output;
  }

  @Override
  public String getResult() throws MediaException {
    try {
      InputCompleteEvent<T> inputCompleteEvent = getInput().get();
      if (inputCompleteEvent != null) {
        return inputCompleteEvent.getValue();
      }
      else {
        return null;
      }
    }
    catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

}

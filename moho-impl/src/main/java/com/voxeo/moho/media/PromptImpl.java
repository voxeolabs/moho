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
import java.util.concurrent.FutureTask;

import com.voxeo.moho.MediaException;
import com.voxeo.moho.event.InputCompleteEvent;

public class PromptImpl implements Prompt {

  protected Output _output;

  protected Input _input;

  protected FutureTask<Input> _future = null;

  protected void setInput(final Input input) {
    _input = input;
  }

  protected void inputGetReady(final Callable<Input> call) {
    _future = new FutureTask<Input>(call);
  }

  protected void inputGetSet() {
    new Thread(_future).start();
  }

  protected void setOutput(final Output output) {
    _output = output;
  }

  @Override
  public Input getInput() throws MediaException {
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
  public Output getOutput() {
    return _output;
  }

  @Override
  public String getResult() throws MediaException {
    try {
      InputCompleteEvent inputCompleteEvent = getInput().get();
      if(inputCompleteEvent != null) {
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

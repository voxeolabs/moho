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

import com.voxeo.moho.MediaException;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.InputCompleteEvent;

public class PromptImpl<T extends EventSource> implements Prompt<T> {

  protected Output<T> _output;

  protected Input<T> _input;

  public PromptImpl() {
  }

  protected void setInput(final Input<T> input) {
    _input = input;
  }

  protected void setOutput(final Output<T> output) {
    _output = output;
  }

  @Override
  public Input<T> getInput() throws MediaException {
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

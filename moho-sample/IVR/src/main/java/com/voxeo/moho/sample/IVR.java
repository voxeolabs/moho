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

package com.voxeo.moho.sample;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.State;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.moho.media.output.OutputCommand.BargeinType;

public class IVR implements Application {

  @Override
  public void init(final ApplicationContext ctx) {
  }

  @Override
  public void destroy() {
  }

  @State
  public void handleInvite(final IncomingCall call) throws Exception {
    call.addObserver(this);
    call.answer();

    call.setApplicationState("menu-level-1");

    OutputCommand output = new OutputCommand(
        "1 for sales, 2 for support, this is the prompt that should be interruptable.");
    output.setBargeinType(BargeinType.ANY);

    InputCommand input = new InputCommand("1,2");
    input.setTerminator('#');
    call.prompt(output, input, 0);
  }

  @State("menu-level-1")
  public void menu1(final InputCompleteEvent<Call> evt) {
    switch (evt.getCause()) {
      case MATCH:
        final Call call = evt.getSource();
        if (evt.getConcept().equals("1")) {
          call.setApplicationState("menu-level-2-1");
          call.prompt("1 for sipmethod, 2 for prophecy", "1,2", 0);
        }
        else {
          call.setApplicationState("menu-level-2-2");
          call.prompt("1 for sipmethod, 2 for prophecy", "1,2", 0);
        }
        break;
    }
  }

  @State("menu-level-2-1")
  public void menu21(final InputCompleteEvent<Call> evt) {
    switch (evt.getCause()) {
      case MATCH:
        final Call call = evt.getSource();
        if (evt.getConcept().equals("1")) {
          call.setApplicationState("menu-simpmethod-sales");
          call.prompt("thank you for calling sipmethod sales", null, 0);
        }
        else {
          call.setApplicationState("menu-prophecy-sales");
          call.prompt("thank you for calling prophecy sales", null, 0);
        }
        break;
    }
  }

  @State("menu-level-2-2")
  public void menu22(final InputCompleteEvent<Call> evt) {
    switch (evt.getCause()) {
      case MATCH:
        final Call call = evt.getSource();
        if (evt.getConcept().equals("1")) {
          call.setApplicationState("menu-simpmethod-support");
          call.prompt("thank you for calling sipmethod support", null, 0);
        }
        else {
          call.setApplicationState("menu-prophecy-support");
          call.prompt("thank you for calling prophecy support", null, 0);
        }
        break;
    }
  }
}

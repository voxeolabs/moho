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

package com.voxeo.moho.sample;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.MediaService;
import com.voxeo.moho.State;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.media.InputMode;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.output.OutputCommand;

public class IVR implements Application {

  @Override
  public void init(final ApplicationContext ctx) {
  }

  @Override
  public void destroy() {
  }

  @State
  public void handleInvite(final Call inv) throws Exception {
    final Call call = inv.answer(this);

    call.setApplicationState("menu-level-1");

    OutputCommand output = new OutputCommand(
        "1 for sales, 2 for support, this is the prompt that should be interruptable.");
    output.setBargein(true);

    final MediaService mg = call.getMediaService(false);
    InputCommand input = new InputCommand("1,2");
    input.setTermChar('#');
    input.setInputMode(InputMode.dtmf);
    input.setSpeechLanguage("en-US");
    mg.prompt(output, input, 0);
  }

  @State("menu-level-1")
  public void menu1(final InputCompleteEvent evt) {
    switch (evt.getCause()) {
      case MATCH:
        final Call call = (Call) evt.getSource();
        if (evt.getConcept().equals("1")) {
          call.setApplicationState("menu-level-2-1");
          call.getMediaService(false).prompt("1 for sipmethod, 2 for prophecy", "1,2", 0);
        }
        else {
          call.setApplicationState("menu-level-2-2");
          call.getMediaService(false).prompt("1 for sipmethod, 2 for prophecy", "1,2", 0);
        }
        break;
    }
  }

  @State("menu-level-2-1")
  public void menu21(final InputCompleteEvent evt) {
    switch (evt.getCause()) {
      case MATCH:
        final Call call = (Call) evt.getSource();
        if (evt.getConcept().equals("1")) {
          call.setApplicationState("menu-simpmethod-sales");
          call.getMediaService(false).prompt("thank you for calling sipmethod sales", null, 0);
        }
        else {
          call.setApplicationState("menu-prophecy-sales");
          call.getMediaService(false).prompt("thank you for calling prophecy sales", null, 0);
        }
        break;
    }
  }

  @State("menu-level-2-2")
  public void menu22(final InputCompleteEvent evt) {
    switch (evt.getCause()) {
      case MATCH:
        final Call call = (Call) evt.getSource();
        if (evt.getConcept().equals("1")) {
          call.setApplicationState("menu-simpmethod-support");
          call.getMediaService(false).prompt("thank you for calling sipmethod support", null, 0);
        }
        else {
          call.setApplicationState("menu-prophecy-support");
          call.getMediaService(false).prompt("thank you for calling prophecy support", null, 0);
        }
        break;
    }
  }
}

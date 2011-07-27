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

import java.util.concurrent.BlockingQueue;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.MediaService;
import com.voxeo.moho.State;
import com.voxeo.moho.event.BlockingQueueEventListener;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.media.Input;
import com.voxeo.moho.media.Output;
import com.voxeo.moho.media.Prompt;
import com.voxeo.moho.media.input.DigitInputCommand;
import com.voxeo.moho.sample.GameServer.Direction;

public class SampleQueue implements Application {

  public void init(final ApplicationContext ctx) {
  }

  public void destroy() {
  }

  private GameServer game;

  @State
  public void handleInvite(final IncomingCall call) throws Exception {

    call.answer();

    game = new DemoGameServer(call);

    final Prompt<Call> agePrompt = call.prompt("Welcome phone sweeper. "
        + "Press 1 if you are over 18, press 2 if you are under 18.", "1,2", 0);

    final Input<Call> input = agePrompt.getInput();
    if (!"1".equals(input.get().getConcept())) {
      final Output<Call> o = call.output("Sorry, you're too young");
      o.get();
      call.disconnect();
      return;
    }

    final MyListener listener = new MyListener();
    final BlockingQueue<InputCompleteEvent<Call>> queue = listener.getQueue();
    call.addObserver(listener);

    call.output("Ready Go");
    call.input(new DigitInputCommand());

    while (true) {
      final InputCompleteEvent<Call> event = queue.take();
      if (event.hasMatch() && event.getConcept() != null) {
        final int command = Integer.parseInt(event.getConcept());
        switch (command) {
          case 2:
            game.move(Direction.UP);
            break;
          case 4:
            game.move(Direction.LEFT);
            break;
          case 6:
            game.move(Direction.RIGHT);
            break;
          case 8:
            game.move(Direction.DOWN);
            break;
        }

      }
    }

  }

  class DemoGameServer implements GameServer {

    private MediaService<Call> _media;

    public DemoGameServer(final MediaService<Call> media) {
      _media = media;
    }

    @Override
    public void move(final Direction direction) {
      _media.output("You will move to " + direction);
      _media.input(new DigitInputCommand());
    }

  }

  class MyListener extends BlockingQueueEventListener<InputCompleteEvent<Call>> {

  }
}

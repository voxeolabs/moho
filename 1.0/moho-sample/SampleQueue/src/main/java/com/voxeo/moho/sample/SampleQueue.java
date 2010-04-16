package com.voxeo.moho.sample;

import java.util.concurrent.BlockingQueue;

import javax.annotation.Resource;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.MediaService;
import com.voxeo.moho.State;
import com.voxeo.moho.event.BlockingQueueEventListener;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.event.InviteEvent;
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

  @Resource
  private GameServer game;

  @State
  public void handleInvite(final InviteEvent invite) throws Exception {

    final Call call = invite.acceptCall();
    call.join().get();

    final MediaService media = call.getMediaService();

    game = new DemoGameServer(media);

    final Prompt agePrompt = media.prompt("Welcome phone sweeper. "
        + "Press 1 if you are over 18, press 2 if you are under 18.", "1,2", 0);

    final Input input = agePrompt.getInput();
    if (!"1".equals(input.get().getConcept())) {
      final Output o = media.output("Sorry, you're too young");
      o.get();
      call.disconnect();
      return;
    }

    final BlockingQueueEventListener<InputCompleteEvent> listener = new BlockingQueueEventListener<InputCompleteEvent>();
    final BlockingQueue<InputCompleteEvent> queue = listener.getQueue();
    call.addListener(InputCompleteEvent.class, listener);

    media.output("Ready Go");
    media.input(new DigitInputCommand());

    while (true) {
      final InputCompleteEvent event = queue.take();
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

    private MediaService _media;

    public DemoGameServer(final MediaService media) {
      _media = media;
    }

    @Override
    public void move(final Direction direction) {
      _media.output("You will move to " + direction);
      _media.input(new DigitInputCommand());
    }

  }

}

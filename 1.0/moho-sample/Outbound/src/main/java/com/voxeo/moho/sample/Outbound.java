package com.voxeo.moho.sample;

import java.util.Timer;
import java.util.TimerTask;

import javax.media.mscontrol.join.Joinable.Direction;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.utils.EventListener;

public class Outbound implements Application {

  CallableEndpoint _party1;

  CallableEndpoint _party2;

  CallableEndpoint _local;

  Timer _timer;

  @Override
  public void init(final ApplicationContext ctx) {
    _party1 = (CallableEndpoint) ctx.getEndpoint(ctx.getParameter("party1"));
    _party2 = (CallableEndpoint) ctx.getEndpoint(ctx.getParameter("party2"));
    _local = (CallableEndpoint) ctx.getEndpoint("sip:mohosample@example.com");
    final long time = Long.parseLong(ctx.getParameter("time"));
    _timer = new Timer();
    _timer.schedule(new TimerTask() {

      @Override
      public void run() {
        final Call c1 = _party1.call(_local, null, (EventListener<?>) null);
        final Call c2 = _party2.call(_local, null, (EventListener<?>) null);
        c1.join(c2, JoinType.DIRECT, Direction.DUPLEX);
      }
    }, time);
  }

  @Override
  public void destroy() {
    if (_timer != null) {
      _timer.cancel();
    }
  }

}

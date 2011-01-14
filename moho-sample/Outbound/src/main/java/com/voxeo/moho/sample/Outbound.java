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
    _party1 = (CallableEndpoint) ctx.createEndpoint(ctx.getParameter("party1"));
    _party2 = (CallableEndpoint) ctx.createEndpoint(ctx.getParameter("party2"));
    _local = (CallableEndpoint) ctx.createEndpoint("sip:mohosample@example.com");
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

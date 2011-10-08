package com.voxeo.moho.sample;

import java.util.ArrayList;
import java.util.List;

import javax.media.mscontrol.join.Joinable.Direction;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.Joint;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.State;
import com.voxeo.moho.Unjoint;
import com.voxeo.moho.event.HangupEvent;

public class JoinStrategy implements Application {

  final List<Call> _calls = new ArrayList<Call>();

  @Override
  public void init(ApplicationContext ctx) {
    // TODO Auto-generated method stub

  }

  @Override
  public void destroy() {
    _calls.clear();
  }

  @State
  public void handleInvite(final IncomingCall call) throws Exception {
    call.addObserver(this);
    call.answer();
    synchronized (_calls) {
      if (_calls.size() > 0) {
        for (Call c : _calls) {
          // Joint joint = call.join(c, JoinType.BRIDGE, Direction.DUPLEX);
          Joint joint = call.join(c, JoinType.BRIDGE_SHARED, Direction.DUPLEX);
          joint.get();
        }
      }
      _calls.add(call);
    }
  }

  @State
  public void handleHangup(final HangupEvent event) throws Exception {
    final Call call = event.getSource();
    synchronized (_calls) {
      _calls.remove(call);
      if (_calls.size() > 0) {
        for (Call c : _calls) {
          Unjoint unjoint = call.unjoin(c);
          unjoint.get();
        }
      }
    }
  }

}

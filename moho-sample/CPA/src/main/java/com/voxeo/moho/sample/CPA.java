package com.voxeo.moho.sample;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.State;
import com.voxeo.moho.cpa.CallProgressAnalyzer;
import com.voxeo.moho.event.CPAEvent;
import com.voxeo.moho.event.CPAEvent.Type;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.event.InputCompleteEvent.Cause;
import com.voxeo.moho.media.input.SignalGrammar.Signal;
import com.voxeo.moho.sip.SIPAnsweredEvent;
import com.voxeo.moho.sip.SIPEarlyMediaEvent;
import com.voxeo.moho.sip.SIPHangupEvent;

public class CPA implements Application {

  private Logger log = Logger.getLogger(CPA.class);

  final CallProgressAnalyzer _analyzer = new CallProgressAnalyzer();

  CallableEndpoint _party;

  Timer _timer;

  Call _call;

  @Override
  public void init(ApplicationContext ctx) {
    _party = (CallableEndpoint) ctx.createEndpoint(ctx.getParameter("To"));
    final long time = Long.parseLong(ctx.getParameter("time"));
    _timer = new Timer();
    _timer.schedule(new TimerTask() {

      @Override
      public void run() {
        _call = _party.createCall("sip:mohosample@example.com");
        _call.addObserver(CPA.this);
        _call.join();
      }
    }, time);
  }

  @Override
  public void destroy() {
    if (_timer != null) {
      _timer.cancel();
    }
    if (_call != null) {
      _analyzer.stop(_call);
      _call.disconnect();
    }
  }

  @State
  public void onCPAEvent(final CPAEvent<Call> event) {
    log.info(event);
    if (event.getType() == Type.HUMAN_DETECTED) {
      log.info("The answering party might be human.");
    }
    else if (event.getType() == Type.MACHINE_DETECTED) {
      if (event.getSignal() != null) {
        log.info("The answering party should be machine.");
      }
      else {
        log.info("The answering party might be machine.");
      }
    }
  }

  @State
  public void onInputComplete(final InputCompleteEvent<Call> event) {
    log.info(event);
    if (event.getCause() == Cause.INI_TIMEOUT) {
      log.warn("The answering party didnt send any media");
    }
  }

  @State
  public void onAnswered(final SIPAnsweredEvent<Call> event) {
    startDetection();
  }

  @State
  public void onEarlyMedia(final SIPEarlyMediaEvent event) {
    startDetection();
  }

  @State
  public void onHangup(final SIPHangupEvent event) {
    _analyzer.stop(_call);
  }

  private void startDetection() {
    _analyzer.start(_call, 60000, 10000, true, new Signal[] {Signal.BEEP, Signal.FAX_CED, Signal.FAX_CNG});
  }

}

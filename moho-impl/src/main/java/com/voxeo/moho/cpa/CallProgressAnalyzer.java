package com.voxeo.moho.cpa;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.voxeo.moho.Call;
import com.voxeo.moho.State;
import com.voxeo.moho.common.event.MohoCPAEvent;
import com.voxeo.moho.event.CPAEvent.Type;
import com.voxeo.moho.event.InputDetectedEvent;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.media.Input;
import com.voxeo.moho.media.input.EnergyGrammar;
import com.voxeo.moho.media.input.Grammar;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.input.SignalGrammar;
import com.voxeo.moho.media.input.SignalGrammar.Signal;

public class CallProgressAnalyzer implements Observer {

  private Logger log = Logger.getLogger(CallProgressAnalyzer.class);

  /**
   * <p>
   * The 'cpa-maxtime' parameter is the "measuring stick" used to determine
   * 'human' or 'machine' events. If the duration of voice activity is less than
   * the value of 'cpa-maxtime', the called party is considered to be 'human.'
   * If voice activity exceeds the 'cpa-maxtime' value, your application has
   * likely called a 'machine'.
   * <p>
   * The recommended value for this parameter is between 4000 and 6000ms.
   */
  protected long voxeo_cpa_max_time = 4000;

  /**
   * <p>
   * The 'cpa-maxsilence' parameter is used to identify the end of voice
   * activity. When activity begins, CPA will measure the duration until a
   * period of silence greater than the value of 'cpa-maxsilence' is detected.
   * Armed with start and end timestamps, CPA can then calculate the total
   * duration of voice activity.
   * <p>
   * A value of 800 to 1200ms is suggested for this parameter.
   */
  protected long voxeo_cpa_final_silence = 1000;

  /**
   * <p>
   * The 'cpa-min-speech-duration' parameter is used to identify the minimum
   * duration of energy.
   * <p>
   * A value of (x)ms to (y)ms is suggested for this parameter.
   */
  protected long voxeo_cpa_min_speech_duration = 80;

  /**
   * <p>
   * The 'cpa-min-volume' parameter is used to identify the threshold of what is
   * considered to be energy vs silence.
   * <p>
   * A value of (x)db to (y)db is suggested for this parameter.
   */
  protected int voxeo_cpa_min_volume = -24;

  protected Map<Call, ProgressStatus> _status = new ConcurrentHashMap<Call, ProgressStatus>();

  public CallProgressAnalyzer() {

  }

  public void setMaxTime(final long voxeo_cpa_max_time) {
    this.voxeo_cpa_max_time = voxeo_cpa_max_time;
  }

  public long getMaxTime() {
    return this.voxeo_cpa_max_time;
  }

  public void setFinalSilence(final long voxeo_cpa_final_silence) {
    this.voxeo_cpa_final_silence = voxeo_cpa_final_silence;
  }

  public long getFinalSilence() {
    return this.voxeo_cpa_final_silence;
  }

  public void setMinSpeechDuration(final long voxeo_cpa_min_speech_duration) {
    this.voxeo_cpa_min_speech_duration = voxeo_cpa_min_speech_duration;
  }

  public long getMinSpeechDuration() {
    return this.voxeo_cpa_min_speech_duration;
  }

  public void setMinVolume(final int voxeo_cpa_min_volume) {
    this.voxeo_cpa_min_volume = voxeo_cpa_min_volume;
  }

  public int getMinVolume() {
    return this.voxeo_cpa_min_volume;
  }

  public void start(final Call call, Signal... signals) {
    start(call, -1, -1, false, signals);
  }

  /**
   * @param call
   * @param runtime
   *          Maximum time duration for detection.
   * @param timeout
   *          Maximum time limit for first media.
   * @param autoreset
   *          Indicating whether detection will contine on receiving
   *          end-of-speech event
   * @param signals
   */
  public void start(final Call call, final long runtime, final long timeout, final boolean autoreset, Signal... signals) {
    call.addObserver(this);
    final Grammar[] grammars = new Grammar[(signals == null || signals.length == 0) ? 2 : signals.length + 2];
    grammars[0] = new EnergyGrammar(true, false, false);
    grammars[1] = new EnergyGrammar(false, true, false);
    if (signals != null && signals.length > 0) {
      for (int i = 0; i < signals.length; i++) {
        grammars[i + 2] = new SignalGrammar(signals[i], false);
      }
    }
    final InputCommand cmd = new InputCommand(grammars);
    if (runtime > 0) {
      cmd.setMaxTimeout(runtime);
    }
    if (timeout > 0) {
      cmd.setInitialTimeout(timeout);
    }
    cmd.setAutoRest(autoreset);
    cmd.setEnergyParameters(voxeo_cpa_final_silence, null, null, voxeo_cpa_min_speech_duration, voxeo_cpa_min_volume);
    log.info("Starting " + this + "[max_time:" + voxeo_cpa_max_time + ", final_silence:" + voxeo_cpa_final_silence
        + ", min_speech:" + voxeo_cpa_min_speech_duration + ", min_volume:" + voxeo_cpa_min_volume + ", runtime:"
        + runtime + ", timeout:" + timeout + ", autoreset:" + autoreset + ", signals=" + toString(signals) + "] on "
        + call);
    final Input<Call> input = call.input(cmd);
    _status.put(call, new ProgressStatus(call, input));
  }

  public void stop(final Call call) {
    ProgressStatus status = _status.remove(call);
    if (status != null) {
      log.info("Stopping " + this + " on " + call);
      status._input.stop();
      call.removeObserver(this);
    }
  }

  @State
  public void onInputDetected(final InputDetectedEvent<Call> event) {
    final Call call = event.getSource();
    final ProgressStatus status = _status.get(call);
    if (status == null) {
      return;
    }
    if (!event.isStartOfSpeech() && !event.isEndOfSpeech() && event.getSignal() == null) {
      return;
    }
    log.info(event);
    InputDetectedEvent<Call> lastEvent = null;
    if (status._events.size() > 0) {
      lastEvent = status._events.get(status._events.size() - 1);
    }
    status._events.add(event);
    if (event.isStartOfSpeech()) {
      status._lastStartOfSpeech = System.currentTimeMillis();
    }
    else if (event.isEndOfSpeech()) {
      if (lastEvent != null && lastEvent.getSignal() != null) {
        status.reset();
        return;
      }
      if (status._lastStartOfSpeech == 0) {
        log.warn("Not received START-OF-SPEECH event yet.");
        status.reset();
        return;
      }

      status._lastEndOfSpeech = System.currentTimeMillis();
      status._retries = +1;
      long duration = status._lastEndOfSpeech - status._lastStartOfSpeech - voxeo_cpa_final_silence;
      if (duration < voxeo_cpa_max_time) {
        call.dispatch(new MohoCPAEvent<Call>(event.getSource(), Type.HUMAN_DETECTED, duration, status._retries));
      }
      else {
        call.dispatch(new MohoCPAEvent<Call>(event.getSource(), Type.MACHINE_DETECTED, duration, status._retries));
      }
      status.reset();
    }
    else if (event.getSignal() != null) {
      call.dispatch(new MohoCPAEvent<Call>(event.getSource(), Type.MACHINE_DETECTED, event.getSignal()));
    }
  }

  private String toString(final Signal[] signals) {
    if (signals == null || signals.length == 0) {
      return null;
    }
    final StringBuilder sbuf = new StringBuilder();
    sbuf.append("[");
    for (final Signal s : signals) {
      sbuf.append(s);
      sbuf.append(",");
    }
    sbuf.replace(sbuf.length() - 1, sbuf.length(), "]");
    return sbuf.toString();
  }

  protected class ProgressStatus {

    protected int _retries = 0;

    protected long _lastStartOfSpeech = 0;

    protected long _lastEndOfSpeech = 0;

    protected final Input<Call> _input;

    protected final Call _call;

    protected final List<InputDetectedEvent<Call>> _events = new ArrayList<InputDetectedEvent<Call>>();

    public ProgressStatus(final Call call, final Input<Call> input) {
      _input = input;
      _call = call;
    }

    public void reset() {
      this._lastStartOfSpeech = 0;
      this._lastEndOfSpeech = 0;
    }
  }

}

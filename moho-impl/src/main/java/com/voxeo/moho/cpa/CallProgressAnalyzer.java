package com.voxeo.moho.cpa;

import org.apache.log4j.Logger;

import com.voxeo.moho.Call;
import com.voxeo.moho.State;
import com.voxeo.moho.event.InputCompleteEvent;
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

  protected long _initialTimeout = -1;

  protected int _retries = 0;

  protected final Call _call;

  protected Input<Call> _input = null;

  private long _lastStartOfSpeech = 0;

  private long _lastEndOfSpeech = 0;

  public CallProgressAnalyzer(final Call call, long voxeo_cpa_max_time, long voxeo_cpa_final_silence,
      long voxeo_cpa_min_speech_duration, int voxeo_cpa_min_volume) {
    _call = call;
    _call.addObserver(this);
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

  public void start(final long runtime, final long timeout, final boolean autoreset, Signal... signals) {
    final Grammar[] grammars = new Grammar[(signals == null || signals.length == 0) ? 2 : signals.length + 2];
    grammars[0] = new EnergyGrammar(true, false, false);
    grammars[1] = new EnergyGrammar(false, true, false);
    if (signals != null && signals.length > 0) {
      for (int i = 0; i < signals.length; i++) {
        grammars[i + 2] = new SignalGrammar(signals[i], false);
      }
    }
    final InputCommand cmd = new InputCommand(grammars);
    cmd.setMaxTimeout(runtime);
    cmd.setInitialTimeout(timeout);
    cmd.setAutoRest(autoreset);
    cmd.setEnergyParameters(voxeo_cpa_final_silence, voxeo_cpa_max_time, null, voxeo_cpa_min_speech_duration,
        voxeo_cpa_min_volume);
    _input = _call.input(cmd);
    _initialTimeout = timeout;
  }

  public void stop() {
    if (_input != null) {
      _input.stop();
    }
  }

  @State
  public void onInputDetected(final InputDetectedEvent<Call> event) {
    log.info(event);
    if (event.isStartOfSpeech()) {
      _lastStartOfSpeech = System.currentTimeMillis();
    }
    else if (event.isEndOfSpeech()) {
      _lastEndOfSpeech = System.currentTimeMillis();

      ++_retries;
      long duration = _lastEndOfSpeech - _lastStartOfSpeech;
      if (duration < voxeo_cpa_max_time) {
        event.getSource().dispatch(new HumanDetectedEvent<Call>(event.getSource(), duration, _retries));
      }
      else {
        event.getSource().dispatch(new MachineDetectedEvent<Call>(event.getSource(), duration, _retries));
      }
      reset();
    }
    else if (event.getSignal() != null) {
      event.getSource().dispatch(new MachineDetectedEvent<Call>(event.getSource(), event.getSignal()));
    }
  }

  @State
  public void onInputComplete(final InputCompleteEvent<Call> event) {
    switch (event.getCause()) {
      case INI_TIMEOUT:
        event.getSource().dispatch(new SilenceDetectedEvent<Call>(event.getSource(), _initialTimeout));
        break;
    }
  }

  private void reset() {
    this._lastStartOfSpeech = 0;
    this._lastEndOfSpeech = 0;
  }
}

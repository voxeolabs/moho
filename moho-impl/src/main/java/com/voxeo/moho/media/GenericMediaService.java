/**
 * Copyright 2010 Voxeo Corporation Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.voxeo.moho.media;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.media.mscontrol.EventType;
import javax.media.mscontrol.MediaEventListener;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameter;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.Qualifier;
import javax.media.mscontrol.mediagroup.MediaGroup;
import javax.media.mscontrol.mediagroup.Player;
import javax.media.mscontrol.mediagroup.PlayerEvent;
import javax.media.mscontrol.mediagroup.Recorder;
import javax.media.mscontrol.mediagroup.RecorderEvent;
import javax.media.mscontrol.mediagroup.SpeechDetectorConstants;
import javax.media.mscontrol.mediagroup.signals.SignalDetector;
import javax.media.mscontrol.mediagroup.signals.SignalDetectorEvent;
import javax.media.mscontrol.mediagroup.signals.SignalGenerator;
import javax.media.mscontrol.mediagroup.signals.SpeechRecognitionEvent;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.resource.RTC;
import javax.media.mscontrol.resource.ResourceEvent;

import org.apache.log4j.Logger;

import com.voxeo.moho.MediaException;
import com.voxeo.moho.MediaService;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.event.InputDetectedEvent;
import com.voxeo.moho.event.OutputCompleteEvent;
import com.voxeo.moho.event.OutputPausedEvent;
import com.voxeo.moho.event.OutputResumedEvent;
import com.voxeo.moho.event.RecordCompleteEvent;
import com.voxeo.moho.event.RecordPausedEvent;
import com.voxeo.moho.event.RecordResumedEvent;
import com.voxeo.moho.event.RecordStartedEvent;
import com.voxeo.moho.event.OutputCompleteEvent.Cause;
import com.voxeo.moho.media.input.Grammar;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.input.SimpleGrammar;
import com.voxeo.moho.media.input.InputCommand.Type;
import com.voxeo.moho.media.output.AudibleResource;
import com.voxeo.moho.media.output.AudioURIResource;
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.moho.media.output.TextToSpeechResource;
import com.voxeo.moho.media.record.RecordCommand;
import com.voxeo.moho.util.NLSMLParser;
import com.voxeo.mscontrol.VoxeoParameter;

public class GenericMediaService implements MediaService {

  private static final Logger LOG = Logger.getLogger(GenericMediaService.class);

  protected EventSource _parent;

  protected MediaSession _session;

  protected MediaGroup _group;

  protected NetworkConnection _call;

  protected Player _player = null;

  protected Recorder _recorder = null;

  protected SignalDetector _detector = null;

  protected SignalGenerator _generator = null;

  protected GenericMediaService(final EventSource parent, final MediaGroup group) {
    _parent = parent;
    _group = group;
    try {
      _generator = _group.getSignalGenerator();
    }
    catch (final MsControlException e) {
      LOG.warn("", e);
    }
    try {
      _recorder = _group.getRecorder();
    }
    catch (final MsControlException e) {
      LOG.warn("", e);
    }
    try {
      _detector = _group.getSignalDetector();
    }
    catch (final MsControlException e) {
      LOG.warn("", e);
    }
    try {
      _player = _group.getPlayer();
    }
    catch (final MsControlException e) {
      LOG.warn("", e);
    }
  }

  public MediaGroup getMediaGroup() {
    return _group;
  }

  @Override
  public Input input(final String grammar) throws MediaException {
    return prompt((String) null, grammar, 0).getInput();
  }

  @Override
  public Input input(final InputCommand input) throws MediaException {
    return prompt(null, input, 0).getInput();
  }

  @Override
  public Output output(final String text) throws MediaException {
    return prompt(text, null, 0).getOutput();
  }

  @Override
  public Output output(final URI media) throws MediaException {
    return prompt(media, null, 0).getOutput();
  }

  @Override
  public Output output(final OutputCommand output) throws MediaException {
    return prompt(output, null, 0).getOutput();
  }

  @Override
  public Prompt prompt(final String text, final String grammar, final int repeat) throws MediaException {
    final OutputCommand output = text == null ? null : new OutputCommand(new TextToSpeechResource(text));
    final InputCommand input = grammar == null ? null : new InputCommand(new SimpleGrammar(grammar));
    return prompt(output, input, repeat);
  }

  @Override
  public Prompt prompt(final URI media, final String grammar, final int repeat) throws MediaException {
    final OutputCommand output = media == null ? null : new OutputCommand(new AudioURIResource(media, null));
    final InputCommand input = grammar == null ? null : new InputCommand(new SimpleGrammar(grammar));
    return prompt(output, input, repeat);
  }

  @Override
  public Prompt prompt(final OutputCommand output, final InputCommand input, final int repeat) throws MediaException {
    final PromptImpl retval = new PromptImpl();
    if (output != null && output.getAudibleResources() != null && output.getAudibleResources().length > 0) {
      final Parameters params = _group.createParameters();
      final List<RTC> rtcs = new ArrayList<RTC>();

      if (output.getParameters() != null) {
        params.putAll(output.getParameters());
      }

      if (output.getRtcs() != null) {
        for (final RTC rtc : output.getRtcs()) {
          rtcs.add(rtc);
        }
      }

      switch (output.getBehavior()) {
        case QUEUE:
          params.put(Player.BEHAVIOUR_IF_BUSY, Player.QUEUE_IF_BUSY);
          break;
        case STOP:
          params.put(Player.BEHAVIOUR_IF_BUSY, Player.STOP_IF_BUSY);
          break;
        case ERROR:
          params.put(Player.BEHAVIOUR_IF_BUSY, Player.FAIL_IF_BUSY);
          break;
      }
      if (output.isBargein()) {
        // rtcs.add(MediaGroup.SIGDET_STOPPLAY);
        rtcs.add(new RTC(SignalDetector.DETECTION_OF_ONE_SIGNAL, Player.STOP_ALL));
        rtcs.add(new RTC(SpeechDetectorConstants.START_OF_SPEECH, Player.STOP_ALL));
        params.put(SpeechDetectorConstants.BARGE_IN_ENABLED, Boolean.TRUE);
      }
      params.put(Player.MAX_DURATION, output.getTimeout());
      params.put(Player.START_OFFSET, output.getOffset());
      params.put(Player.VOLUME_CHANGE, output.getVolumeUnit());
      params.put(Player.AUDIO_CODEC, output.getCodec());
      params.put(Player.FILE_FORMAT, output.getFormat());
      params.put(Player.JUMP_PLAYLIST_INCREMENT, output.getJumpPlaylistIncrement());
      params.put(Player.JUMP_TIME, output.getJumpTime());
      params.put(Player.START_IN_PAUSED_MODE, output.isStartInPausedMode());
      if (repeat > 0) {
        params.put(Player.REPEAT_COUNT, repeat + 1);
        params.put(Player.INTERVAL, output.getRepeatInterval());
      }
      final List<URI> uris = new ArrayList<URI>();
      final MediaResource[] reses = output.getAudibleResources();
      for (final MediaResource r : reses) {
        uris.add(r.toURI());
      }

      try {
        if (input != null) {
          if (uris.size() > 0) {
            params.put(SignalDetector.PROMPT, uris.toArray(new URI[] {}));
          }
          input.setParameters(params);
          input.setRtcs(rtcs.toArray(new RTC[] {}));

          retval.inputGetReady(new SignalDetectorWorker(input));
          retval.inputGetSet();
        }
        else {
          final OutputImpl out = new OutputImpl(_group);
          _player.addListener(new PlayerListener(out, input == null ? null : retval));
          _player.play(uris.toArray(new URI[] {}), rtcs.toArray(new RTC[] {}), params);
          retval.setOutput(out.prepare());
        }
      }
      catch (final MsControlException e) {
        throw new MediaException(e);
      }
    }
    else {
      retval.setInput(detectSignal(input));
    }
    return retval;
  }

  @Override
  public Recording record(final URI recording) throws MediaException {
    final RecordingImpl retval = new RecordingImpl(_group);
    try {
      _recorder.addListener(new RecorderListener(retval));
      _recorder.record(recording, RTC.NO_RTC, Parameters.NO_PARAMETER);
      retval.prepare();
      return retval;
    }
    catch (final Exception e) {
      throw new MediaException(e);
    }
  }

  @Override
  public Recording record(final RecordCommand command) throws MediaException {
    final RecordingImpl retval = new RecordingImpl(_group);
    try {
      final List<RTC> rtcs = new ArrayList<RTC>();

      final Parameters params = _group.createParameters();
      if (!command.isSignalTruncationOn()) {
        params.put(Recorder.SIGNAL_TRUNCATION_ON, Boolean.FALSE);
      }
      if (command.isAppend()) {
        params.put(Recorder.APPEND, Boolean.TRUE);
      }
      if (command.getAudioClockRate() > 0) {
        params.put(Recorder.AUDIO_CLOCKRATE, command.getAudioClockRate());
      }
      if (command.getAudioCODEC() != null) {
        params.put(Recorder.AUDIO_CODEC, command.getAudioCODEC());
      }
      if (command.getAudioFMTP() != null) {
        params.put(Recorder.AUDIO_FMTP, command.getAudioFMTP());
      }
      if (command.getAudioMaxBitRate() > 0) {
        params.put(Recorder.AUDIO_MAX_BITRATE, command.getAudioMaxBitRate());
      }
      if (command.isStartBeep()) {
        params.put(Recorder.START_BEEP, Boolean.TRUE);
        if (command.getBeepFrequency() > 0) {
          params.put(Recorder.BEEP_FREQUENCY, command.getBeepFrequency());
        }
        if (command.getBeepLength() > 0) {
          params.put(Recorder.BEEP_LENGTH, command.getBeepLength());
        }
      }
      else {
        params.put(Recorder.START_BEEP, Boolean.FALSE);
      }
      if (command.isStartInPausedMode()) {
        params.put(Recorder.START_IN_PAUSED_MODE, Boolean.TRUE);
      }
      if (command.getFileFormat() != null) {
        params.put(Recorder.FILE_FORMAT, command.getFileFormat());
      }
      if (command.getMaxDuration() > 0) {
        params.put(Recorder.MAX_DURATION, command.getMaxDuration());
      }
      if (command.getMinDuration() > 0) {
        params.put(Recorder.MIN_DURATION, command.getMinDuration());
      }
      if (command.getPrompt() != null) {
        final AudibleResource[] resources = command.getPrompt().getAudibleResources();
        if (resources.length > 0) {
          final URI[] uris = new URI[resources.length];

          for (int i = 0; i < resources.length; i++) {
            uris[i] = resources[i].toURI();
          }
          params.put(Recorder.PROMPT, uris);
        }

        if (command.getPrompt().isBargein()) {
          params.put(SpeechDetectorConstants.BARGE_IN_ENABLED, Boolean.TRUE);
        }
      }
      if (command.isSilenceTerminationOn()) {
        params.put(Recorder.SILENCE_TERMINATION_ON, Boolean.TRUE);
      }

      if (command.getSpeechDetectionMode() != null) {
        switch (command.getSpeechDetectionMode()) {
          case DETECTOR_INACTIVE:
            params.put(Recorder.SPEECH_DETECTION_MODE, Recorder.DETECTOR_INACTIVE);
            break;
          case DETECT_FIRST_OCCURRENCE:
            params.put(Recorder.SPEECH_DETECTION_MODE, Recorder.DETECT_FIRST_OCCURRENCE);
            break;
          case DETECT_ALL_OCCURRENCES:
            params.put(Recorder.SPEECH_DETECTION_MODE, Recorder.DETECT_ALL_OCCURRENCES);
        }
      }

      if (command.getInitialTimeout() > 0 || command.getFinalTimeout() > 0) {
        // params.put(Recorder.SPEECH_DETECTION_MODE,
        // Recorder.DETECT_ALL_OCCURRENCES);
        if (command.getInitialTimeout() > 0) {
          params.put(SpeechDetectorConstants.INITIAL_TIMEOUT, command.getInitialTimeout());
        }
        if (command.getFinalTimeout() > 0) {
          params.put(Recorder.SILENCE_TERMINATION_ON, Boolean.TRUE);
          params.put(SpeechDetectorConstants.FINAL_TIMEOUT, command.getFinalTimeout());
        }
      }

      if (command.getVideoCODEC() != null) {
        params.put(Recorder.VIDEO_CODEC, command.getVideoCODEC());
      }
      if (command.getVideoFMTP() != null) {
        params.put(Recorder.VIDEO_FMTP, command.getVideoFMTP());
      }
      if (command.getVideoMaxBitRate() > 0) {
        params.put(Recorder.VIDEO_MAX_BITRATE, command.getVideoMaxBitRate());
      }

      _recorder.addListener(new RecorderListener(retval));
      _recorder.record(command.getRecordURI(), rtcs.toArray(new RTC[] {}), params);
      retval.prepare();
      return retval;
    }
    catch (final Exception e) {
      throw new MediaException(e);
    }
  }

  protected Input detectSignal(final InputCommand cmd) throws MediaException {
    if (cmd.isRecord()) {
      try {
        _recorder.record(cmd.getRecordURI(), cmd.getRtcs() != null ? cmd.getRtcs() : RTC.NO_RTC,
            cmd.getParameters() != null ? cmd.getParameters() : Parameters.NO_PARAMETER);
      }
      catch (final Exception e) {
        throw new MediaException(e);
      }
    }
    final Parameters params = _group.createParameters();
    if (cmd.getParameters() != null) {
      params.putAll(cmd.getParameters());
    }

    final List<RTC> rtcs = new ArrayList<RTC>();
    if (cmd.getRtcs() != null) {
      for (final RTC rtc : cmd.getRtcs()) {
        rtcs.add(rtc);
      }
    }

    params.put(SignalDetector.BUFFERING, cmd.isBuffering());
    params.put(SignalDetector.MAX_DURATION, cmd.getMaxTimeout());
    params.put(SignalDetector.INITIAL_TIMEOUT, cmd.getInitialTimeout());
    params.put(SignalDetector.INTER_SIG_TIMEOUT, cmd.getInterSigTimeout());
    if (cmd.getType() != Type.DTMF) {
      params.put(SpeechDetectorConstants.SENSITIVITY, cmd.getConfidence());
    }

    Parameter[] patternKeys = null;

    final Grammar[] grammars = cmd.getGrammars();
    if (grammars.length > 0) {
      final List<Object> patterns = new ArrayList<Object>(grammars.length);
      for (final Grammar grammar : grammars) {
        if (grammar == null) {
          continue;
        }
        Object o = grammar.toURI();
        if (o == null) {
          final String text = grammar.toText();
          try {
            o = new URL(text);
          }
          catch (final MalformedURLException e) {
            o = text;
          }
        }
        if (o == null) {
          continue;
        }
        patterns.add(o);
      }

      final Parameters patternParams = _group.createParameters();
      patternKeys = new Parameter[patterns.size()];
      int i = 0;
      for (; i < patterns.size(); i++) {
        final Object o = patterns.get(i);
        patternKeys[i] = SignalDetector.PATTERN[i];
        patternParams.put(SignalDetector.PATTERN[i], o);
      }

      // process terminate char.
      if (cmd.getTerminateChar() != null) {
        patternParams.put(VoxeoParameter.DTMF_TERM_CHAR, cmd.getTerminateChar());
      }

      if (patterns.size() > 0) {
        _group.setParameters(patternParams);
      }
    }

    if (patternKeys == null && cmd.getSignalNumber() == -1) {
      throw new MediaException("No pattern");
    }

    final InputImpl in = new InputImpl(_group);
    _detector.addListener(new DetectorListener(in, cmd));
    try {
      _detector.receiveSignals(cmd.getSignalNumber(), patternKeys, rtcs.toArray(new RTC[] {}), params);
    }
    catch (final MsControlException e) {
      throw new MediaException(e);
    }
    in.prepare();
    return in;
  }

  protected class SignalDetectorWorker implements Callable<Input> {

    private InputCommand _inputCmd = null;

    public SignalDetectorWorker(final InputCommand inputCmd) {
      _inputCmd = inputCmd;
    }

    @Override
    public Input call() throws MediaException {
      return detectSignal(_inputCmd);
    }

  }

  protected class PlayerListener implements MediaEventListener<PlayerEvent> {

    private OutputImpl _output = null;

    private PromptImpl _prompt = null;

    public PlayerListener(final OutputImpl output, final PromptImpl prompt) {
      _output = output;
      _prompt = prompt;
    }

    @Override
    public void onEvent(final PlayerEvent e) {
      final EventType t = e.getEventType();
      if (t == PlayerEvent.PLAY_COMPLETED) {
        _player.removeListener(this);
        OutputCompleteEvent.Cause cause = Cause.UNKNOWN;
        final Qualifier q = e.getQualifier();
        if (q == PlayerEvent.END_OF_PLAY_LIST) {
          cause = Cause.END;
        }
        else if (q == PlayerEvent.DURATION_EXCEEDED) {
          cause = Cause.TIMEOUT;
        }
        else if (q == ResourceEvent.RTC_TRIGGERED) {
          if (e.getRTCTrigger() == MediaGroup.SIGDET_STOPPLAY) {
            cause = Cause.BARGEIN;
          }
        }
        else if (q == ResourceEvent.STOPPED) {
          cause = Cause.CANCEL;
        }
        final OutputCompleteEvent outputCompleteEvent = new OutputCompleteEvent(_parent, cause);
        _output.done(outputCompleteEvent);
        _parent.dispatch(outputCompleteEvent);
        if (_prompt != null) {
          _prompt.inputGetSet();
        }
      }
      else if (t == PlayerEvent.PAUSED) {
        _parent.dispatch(new OutputPausedEvent(_parent));
      }
      else if (t == PlayerEvent.RESUMED) {
        _parent.dispatch(new OutputResumedEvent(_parent));
      }
    }
  }

  protected class DetectorListener implements MediaEventListener<SignalDetectorEvent> {

    private InputImpl _input = null;

    private InputCommand _cmd = null;

    public DetectorListener(final InputImpl input, final InputCommand inputCmd) {
      _input = input;
      _cmd = inputCmd;
    }

    @Override
    public void onEvent(final SignalDetectorEvent e) {
      final EventType t = e.getEventType();
      if (t == SignalDetectorEvent.RECEIVE_SIGNALS_COMPLETED) {
        _detector.removeListener(this);
        if (_cmd.isRecord()) {
          _recorder.stop();
        }
        InputCompleteEvent.Cause cause = InputCompleteEvent.Cause.UNKNOWN;
        final Qualifier q = e.getQualifier();
        if (q == SignalDetectorEvent.DURATION_EXCEEDED) {
          cause = InputCompleteEvent.Cause.MAX_TIMEOUT;
        }
        else if (q == SignalDetectorEvent.INITIAL_TIMEOUT_EXCEEDED) {
          cause = InputCompleteEvent.Cause.INI_TIMEOUT;
        }
        else if (q == SignalDetectorEvent.INTER_SIG_TIMEOUT_EXCEEDED) {
          cause = InputCompleteEvent.Cause.IS_TIMEOUT;
        }
        else if (q == SpeechRecognitionEvent.NO_GRAMMAR_MATCH) {
          cause = InputCompleteEvent.Cause.NO_MATCH;
        }
        else if (q == ResourceEvent.STOPPED) {
          cause = InputCompleteEvent.Cause.CANCEL;
        }
        else if (q == SignalDetectorEvent.NUM_SIGNALS_DETECTED || patternMatched(e)) {
          cause = InputCompleteEvent.Cause.MATCH;
        }
        else if (_cmd.getTerminateChar() != null && e.getQualifier() == ResourceEvent.RTC_TRIGGERED) {
          cause = InputCompleteEvent.Cause.CANCEL;
        }
        final InputCompleteEvent inputCompleteEvent = new InputCompleteEvent(_parent, cause);
        if (e instanceof SpeechRecognitionEvent) {
          final SpeechRecognitionEvent se = (SpeechRecognitionEvent) e;
          inputCompleteEvent.setUtterance(se.getUserInput());
          try {
            inputCompleteEvent.setNlsml(se.getSemanticResult().getPath());
            final List<Map<String, String>> nlsml = NLSMLParser.parse(inputCompleteEvent.getNlsml());
            for (final Map<String, String> reco : nlsml) {
              final String conf = reco.get("_confidence");
              if (conf != null) {
                inputCompleteEvent.setConfidence(Float.parseFloat(conf));
              }
              final String interpretation = reco.get("_interpretation");
              if (interpretation != null) {
                inputCompleteEvent.setInterpretation(interpretation);
              }
            }
          }
          catch (final Exception e1) {
            LOG.warn("No NLSML", e1);
          }
        }
        else {
          inputCompleteEvent.setConcept(e.getSignalString());
          inputCompleteEvent.setConfidence(1.0F);
        }
        _input.done(inputCompleteEvent);
        if (_cmd.isSupervised()) {
          _parent.dispatch(inputCompleteEvent);
        }
      }
      else if (t == SignalDetectorEvent.SIGNAL_DETECTED) {
        if (_cmd.isSupervised()) {
          _parent.dispatch(new InputDetectedEvent(_parent, e.getSignalString()));
        }
      }
    }
  }

  private boolean patternMatched(final SignalDetectorEvent event) {
    for (final Qualifier q : SignalDetectorEvent.PATTERN_MATCHING) {
      if (event.getQualifier() == q) {
        return true;
      }
    }

    return false;
  }

  protected class RecorderListener implements MediaEventListener<RecorderEvent> {

    private RecordingImpl _recording = null;

    public RecorderListener(final RecordingImpl recording) {
      _recording = recording;
    }

    @Override
    public void onEvent(final RecorderEvent e) {
      final EventType t = e.getEventType();
      if (t == RecorderEvent.RECORD_COMPLETED) {
        _recorder.removeListener(this);
        RecordCompleteEvent.Cause cause = RecordCompleteEvent.Cause.UNKNOWN;
        final Qualifier q = e.getQualifier();
        if (q == RecorderEvent.DURATION_EXCEEDED) {
          cause = RecordCompleteEvent.Cause.TIMEOUT;
        }
        else if (q == RecorderEvent.SILENCE) {
          cause = RecordCompleteEvent.Cause.SILENCE;
        }
        else if (q == SpeechDetectorConstants.INITIAL_TIMEOUT_EXPIRED) {
          cause = RecordCompleteEvent.Cause.INI_TIMEOUT;
        }
        else if (q == ResourceEvent.STOPPED) {
          cause = RecordCompleteEvent.Cause.CANCEL;
        }
        final RecordCompleteEvent recordCompleteEvent = new RecordCompleteEvent(_parent, cause, e.getDuration());
        _recording.done(recordCompleteEvent);
        _parent.dispatch(recordCompleteEvent);
      }
      else if (t == RecorderEvent.PAUSED) {
        _parent.dispatch(new RecordPausedEvent(_parent));
      }
      else if (t == RecorderEvent.RESUMED) {
        _parent.dispatch(new RecordResumedEvent(_parent));
      }
      else if (t == RecorderEvent.STARTED) {
        _parent.dispatch(new RecordStartedEvent(_parent));
      }
    }
  }

}

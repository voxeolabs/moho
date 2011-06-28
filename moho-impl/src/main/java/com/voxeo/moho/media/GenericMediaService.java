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
package com.voxeo.moho.media;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
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
import javax.media.mscontrol.UnsupportedException;
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

import com.voxeo.moho.ExecutionContext;
import com.voxeo.moho.MediaException;
import com.voxeo.moho.MediaService;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.event.InputDetectedEvent;
import com.voxeo.moho.event.MediaCompleteEvent;
import com.voxeo.moho.event.OutputCompleteEvent;
import com.voxeo.moho.event.OutputCompleteEvent.Cause;
import com.voxeo.moho.event.OutputPausedEvent;
import com.voxeo.moho.event.OutputResumedEvent;
import com.voxeo.moho.event.RecordCompleteEvent;
import com.voxeo.moho.event.RecordPausedEvent;
import com.voxeo.moho.event.RecordResumedEvent;
import com.voxeo.moho.event.RecordStartedEvent;
import com.voxeo.moho.media.dialect.MediaDialect;
import com.voxeo.moho.media.input.Grammar;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.input.SimpleGrammar;
import com.voxeo.moho.media.output.AudibleResource;
import com.voxeo.moho.media.output.AudioURIResource;
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.moho.media.output.TextToSpeechResource;
import com.voxeo.moho.media.record.RecordCommand;
import com.voxeo.moho.util.NLSMLParser;

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

  protected ExecutionContext _context;

  protected MediaDialect _dialect;

  protected List<MediaOperation<? extends MediaCompleteEvent>> futures = new LinkedList<MediaOperation<? extends MediaCompleteEvent>>();

  protected GenericMediaService(final EventSource parent, final MediaGroup group, final MediaDialect dialect) {
    _parent = parent;
    _group = group;
    _dialect = dialect;
    _context = (ExecutionContext) _parent.getApplicationContext();
  }

  protected synchronized Player getPlayer() {
    if (_player == null) {
      try {
        _player = _group.getPlayer();
      }
      catch (UnsupportedException ex) {
        LOG.debug("", ex);
        throw new UnsupportedOperationException("player is not supported by " + _group);
      }
      catch (MsControlException e) {
        LOG.error("", e);
        throw new MediaException(e);
      }

      if (_player == null) {
        throw new UnsupportedOperationException("Can't get Player.");
      }
    }

    return _player;
  }

  protected synchronized Recorder getRecorder() {
    if (_recorder == null) {
      try {
        _recorder = _group.getRecorder();
      }
      catch (UnsupportedException ex) {
        LOG.debug("", ex);
        throw new UnsupportedOperationException("Recorder is not supported by " + _group);
      }
      catch (MsControlException e) {
        LOG.error("", e);
        throw new MediaException(e);
      }

      if (_recorder == null) {
        throw new UnsupportedOperationException("Can't get Recorder.");
      }
    }

    return _recorder;
  }

  protected synchronized SignalDetector getSignalDetector() {
    if (_detector == null) {
      try {
        _detector = _group.getSignalDetector();
      }
      catch (UnsupportedException ex) {
        LOG.debug("", ex);
        throw new UnsupportedOperationException("SignalDetector is not supported by " + _group);
      }
      catch (MsControlException e) {
        LOG.error("", e);
        throw new MediaException(e);
      }

      if (_detector == null) {
        throw new UnsupportedOperationException("Can't get SignalDetector.");
      }
    }

    return _detector;
  }

  protected synchronized SignalGenerator getSignalGenerator() {
    if (_generator == null) {
      try {
        _generator = _group.getSignalGenerator();
      }
      catch (UnsupportedException ex) {
        LOG.debug("", ex);
        throw new UnsupportedOperationException("SignalGenerator is not supported by " + _group);
      }
      catch (MsControlException e) {
        LOG.error("", e);
        throw new MediaException(e);
      }

      if (_generator == null) {
        throw new UnsupportedOperationException("Can't get SignalGenerator.");
      }
    }

    return _generator;
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
    final OutputCommand output = media == null ? null : new OutputCommand(new AudioURIResource(media));
    final InputCommand input = grammar == null ? null : new InputCommand(new SimpleGrammar(grammar));
    return prompt(output, input, repeat);
  }

  @SuppressWarnings("deprecation")
  @Override
  public Prompt prompt(final OutputCommand output, final InputCommand input, final int repeat) throws MediaException {
    final PromptImpl retval = new PromptImpl(_context);
    if (output != null && output.getAudibleResources() != null && output.getAudibleResources().length > 0) {
      final Parameters params = _group.createParameters();
      final List<RTC> rtcs = new ArrayList<RTC>();

      if (output.getParameters() != null) {
        params.putAll(output.getParameters());
      }

      if (output.size() > 0) {
        params.putAll(output);
      }

      if (output.getRtcs() != null) {
        for (final RTC rtc : output.getRtcs()) {
          rtcs.add(rtc);
        }
      }

      if (output.getAllRTC() != null && output.getAllRTC().size() > 0) {
        rtcs.addAll(output.getAllRTC());
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
      else {
        params.put(SpeechDetectorConstants.BARGE_IN_ENABLED, Boolean.FALSE);
      }
      params.put(Player.MAX_DURATION, output.getTimeout());
      params.put(Player.START_OFFSET, output.getOffset());
      params.put(Player.VOLUME_CHANGE, output.getVolumeUnit());
      params.put(Player.AUDIO_CODEC, output.getCodec());
      params.put(Player.FILE_FORMAT, output.getFormat());
      params.put(Player.JUMP_PLAYLIST_INCREMENT, output.getJumpPlaylistIncrement());
      params.put(Player.JUMP_TIME, output.getJumpTime());
      params.put(Player.START_IN_PAUSED_MODE, output.isStartInPausedMode());

      params.put(Player.ENABLED_EVENTS, new EventType[] {PlayerEvent.SPEED_CHANGED, PlayerEvent.VOLUME_CHANGED});

      _dialect.setTextToSpeechVoice(params, output.getVoiceName());

      if (output.getRepeatTimes() > 0) {
        params.put(Player.REPEAT_COUNT, output.getRepeatTimes() + 1);
        params.put(Player.INTERVAL, output.getRepeatInterval());
      }

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
          if (input.getParameters() != null) {
            input.getParameters().putAll(params);
          }
          else {
            input.setParameters(params);
          }

          if (input.getRtcs() != null && input.getRtcs().length > 0) {
            RTC[] inputRTCs = input.getRtcs();
            for (RTC rtc : inputRTCs) {
              rtcs.add(rtc);
            }
          }
          input.setRtcs(rtcs.toArray(new RTC[] {}));

          retval.inputGetReady(new SignalDetectorWorker(input));
          retval.inputGetSet();
          futures.add(retval.getInput());
        }
        else {
          final OutputImpl out = new OutputImpl(_group);
          getPlayer().addListener(new PlayerListener(out, null));
          getPlayer().play(uris.toArray(new URI[] {}), rtcs.toArray(new RTC[] {}), params);
          retval.setOutput(out);
          futures.add(out);
        }
      }
      catch (final MsControlException e) {
        throw new MediaException(e);
      }
    }
    else {
      Input futureInput = detectSignal(input);
      retval.setInput(futureInput);
      futures.add(futureInput);
    }
    return retval;
  }

  @Override
  public Recording record(final URI recording) throws MediaException {
    final RecordingImpl retval = new RecordingImpl(_group);
    try {
      getRecorder().addListener(new RecorderListener(retval));
      getRecorder().record(recording, RTC.NO_RTC, Parameters.NO_PARAMETER);
      futures.add(retval);
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

      if (command.getFinishOnKey() != null) {
        params.put(SignalDetector.PATTERN[0], command.getFinishOnKey());
        rtcs.add(new RTC(SignalDetector.PATTERN_MATCH[0], Recorder.STOP));
      }

      getRecorder().addListener(new RecorderListener(retval));
      getRecorder().record(command.getRecordURI(), rtcs.toArray(new RTC[] {}), params);
      futures.add(retval);
      return retval;
    }
    catch (final Exception e) {
      throw new MediaException(e);
    }
  }

  @SuppressWarnings("deprecation")
  protected Input detectSignal(final InputCommand cmd) throws MediaException {

    if (cmd.isRecord()) {
      try {
        getRecorder().record(cmd.getRecordURI(), cmd.getRtcs() != null ? cmd.getRtcs() : RTC.NO_RTC,
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

    if (cmd.size() > 0) {
      params.putAll(cmd);
    }

    if (cmd.getAllRTC() != null && cmd.getAllRTC().size() > 0) {
      rtcs.addAll(cmd.getAllRTC());
    }

    params.put(SignalDetector.BUFFERING, cmd.isBuffering());
    params.put(SignalDetector.MAX_DURATION, cmd.getMaxTimeout());
    params.put(SignalDetector.INITIAL_TIMEOUT, cmd.getInitialTimeout());
    params.put(SignalDetector.INTER_SIG_TIMEOUT, cmd.getInterSigTimeout());
    params.put(SpeechDetectorConstants.SENSITIVITY, cmd.getSensitivity());

    if (cmd.isSupervised()) {
      params.put(SignalDetector.ENABLED_EVENTS, new EventType[] {SignalDetectorEvent.SIGNAL_DETECTED});
    }

    _dialect.setSpeechLanguage(params, cmd.getSpeechLanguage());
    _dialect.setSpeechTermChar(params, cmd.getTermChar());
    _dialect.setSpeechInputMode(params, cmd.getInputMode());
    _dialect.setDtmfHotwordEnabled(params, cmd.isDtmfHotword());
    _dialect.setDtmfTypeaheadEnabled(params, cmd.isDtmfTypeahead());
    _dialect.setConfidence(params, cmd.getConfidence());

    Parameter[] patternKeys = null;

    final Grammar[] grammars = cmd.getGrammars();
    if (grammars.length > 0) {
      final List<Object> patterns = new ArrayList<Object>(grammars.length);
      for (final Grammar grammar : grammars) {
        if (grammar == null) {
          continue;
        }

        Object pattern = null;

        URI uri = grammar.toURI();

        if ("data".equals(uri.getScheme())) {
          pattern = uri;
        }
        else if ("digits".equals(uri.getScheme())) {
          try {
            pattern = URLDecoder.decode(uri.getSchemeSpecificPart(), "UTF-8");
          }
          catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
          }
        }
        else {
          try {
            pattern = uri.toURL();
          }
          catch (MalformedURLException e) {
            LOG.warn("Skipped Grammar! Only 'data' URIs and http/https/ftp/file URLs are permitted [uri="
                + uri.toString() + "]");
          }
        }

        patterns.add(pattern);

      }

      final Parameters patternParams = _group.createParameters();
      patternKeys = new Parameter[patterns.size()];
      int i = 0;
      for (; i < patterns.size(); i++) {
        final Object o = patterns.get(i);
        patternKeys[i] = SignalDetector.PATTERN[i];
        patternParams.put(SignalDetector.PATTERN[i], o);
      }

      if (patterns.size() > 0) {
        _group.setParameters(patternParams);
      }
    }

    if (patternKeys == null && cmd.getSignalNumber() == -1) {
      throw new MediaException("No pattern");
    }

    final InputImpl in = new InputImpl(_group);
    getSignalDetector().addListener(new DetectorListener(in, cmd));
    try {
      getSignalDetector().receiveSignals(cmd.getSignalNumber(), patternKeys, rtcs.toArray(new RTC[] {}), params);
    }
    catch (final MsControlException e) {
      throw new MediaException(e);
    }
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
        getPlayer().removeListener(this);
        OutputCompleteEvent.Cause cause = Cause.UNKNOWN;
        final Qualifier q = e.getQualifier();
        if (q == PlayerEvent.END_OF_PLAY_LIST) {
          cause = Cause.END;
        }
        else if (q == PlayerEvent.DURATION_EXCEEDED) {
          cause = Cause.TIMEOUT;
        }
        else if (q == ResourceEvent.RTC_TRIGGERED) {
          if (e.getRTCTrigger() == MediaGroup.SIGDET_STOPPLAY.getTrigger()) {
            cause = Cause.BARGEIN;
          }
          // for _group.triggerAction(Player.STOP);
          else if (e.getRTCTrigger() == ResourceEvent.MANUAL_TRIGGER) {
            cause = Cause.CANCEL;
          }
        }
        else if (q == ResourceEvent.STOPPED) {
          if (_output.isNormalDisconnect()) {
            cause = Cause.DISCONNECT;
          }
          else {
            cause = Cause.CANCEL;
          }
        }
        final OutputCompleteEvent outputCompleteEvent = new OutputCompleteEvent(_parent, cause);
        _parent.dispatch(outputCompleteEvent);
        _output.done(outputCompleteEvent);
        if (_prompt != null) {
          _prompt.inputGetSet();
        }
      }
      else if (t == PlayerEvent.PAUSED) {
        _output.pauseActionDone();
        _parent.dispatch(new OutputPausedEvent(_parent));
      }
      else if (t == PlayerEvent.RESUMED) {
        _output.resumeActionDone();
        _parent.dispatch(new OutputResumedEvent(_parent));
      }
      else if (t == PlayerEvent.SPEED_CHANGED) {
        _output.speedActionDone();
      }
      else if (t == PlayerEvent.VOLUME_CHANGED) {
        _output.volumeActionDone();
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
        getSignalDetector().removeListener(this);
        if (_cmd.isRecord()) {
          getRecorder().stop();
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
          if (_input.isNormalDisconnect()) {
            cause = InputCompleteEvent.Cause.DISCONNECT;
          }
          else {
            cause = InputCompleteEvent.Cause.CANCEL;
          }
        }
        else if (q == SignalDetectorEvent.NUM_SIGNALS_DETECTED || patternMatched(e)) {
          cause = InputCompleteEvent.Cause.MATCH;
        }
        else if (e.getQualifier() == ResourceEvent.RTC_TRIGGERED) {
          cause = InputCompleteEvent.Cause.CANCEL;
        }
        final InputCompleteEvent inputCompleteEvent = new InputCompleteEvent(_parent, cause);
        if (e instanceof SpeechRecognitionEvent) {
          String signalString = e.getSignalString();
          if (signalString != null) {
            inputCompleteEvent.setConcept(signalString);
            inputCompleteEvent.setConfidence(1.0F);
            inputCompleteEvent.setInterpretation(signalString);
            inputCompleteEvent.setUtterance(signalString);
            inputCompleteEvent.setInputMode(InputMode.dtmf);
          }
          else {
            final SpeechRecognitionEvent se = (SpeechRecognitionEvent) e;
            inputCompleteEvent.setUtterance(se.getUserInput());
            inputCompleteEvent.setTag(se.getTag());
            inputCompleteEvent.setConcept(se.getTag());
            final URL semanticResult = se.getSemanticResult();
            if (semanticResult != null && "application/x-nlsml".equalsIgnoreCase(semanticResult.getHost())) {
              try {
                inputCompleteEvent.setNlsml(semanticResult.getPath());
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
                  final String inputmode = reco.get("_inputmode");
                  if (inputmode != null) {
                    if (inputmode.equalsIgnoreCase("speech") || inputmode.equalsIgnoreCase("voice")) {
                      inputCompleteEvent.setInputMode(InputMode.voice);
                    }
                    else {
                      inputCompleteEvent.setInputMode(InputMode.dtmf);
                    }
                  }
                }
              }
              catch (final Exception e1) {
                LOG.warn("No NLSML", e1);
              }
            }
          }
        }
        else {
          String signalString = e.getSignalString();
          inputCompleteEvent.setConcept(signalString);
          inputCompleteEvent.setConfidence(1.0F);
          inputCompleteEvent.setInterpretation(signalString);
          inputCompleteEvent.setUtterance(signalString);
          inputCompleteEvent.setInputMode(InputMode.dtmf);
        }
        if (_cmd.isSupervised()) {
          _parent.dispatch(inputCompleteEvent);
        }
        _input.done(inputCompleteEvent);
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
        getRecorder().removeListener(this);
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
          if (_recording.isNormalDisconnect()) {
            cause = RecordCompleteEvent.Cause.DISCONNECT;
          }
          else {
            cause = RecordCompleteEvent.Cause.CANCEL;
          }
        }
        else if (q == ResourceEvent.RTC_TRIGGERED) {
          cause = RecordCompleteEvent.Cause.CANCEL;
        }
        final RecordCompleteEvent recordCompleteEvent = new RecordCompleteEvent(_parent, cause, e.getDuration());
        _parent.dispatch(recordCompleteEvent);
        _recording.done(recordCompleteEvent);
      }
      else if (t == RecorderEvent.PAUSED) {
        _recording.pauseActionDone();
        _parent.dispatch(new RecordPausedEvent(_parent));
      }
      else if (t == RecorderEvent.RESUMED) {
        _recording.resumeActionDone();
        _parent.dispatch(new RecordResumedEvent(_parent));
      }
      else if (t == RecorderEvent.STARTED) {
        _parent.dispatch(new RecordStartedEvent(_parent));
      }
    }
  }

  public void release(boolean isNormalDisconnect) {
    Iterator<MediaOperation<? extends MediaCompleteEvent>> ite = futures.iterator();

    while (ite.hasNext()) {
      MediaOperation<?> future = ite.next();

      if (future instanceof RecordingImpl) {
        RecordingImpl recording = (RecordingImpl) future;
        if (recording.isPending()) {
          recording.normalDisconnect(isNormalDisconnect);
        }
        recording.pauseActionDone();
        recording.resumeActionDone();
      }
      else if (future instanceof InputImpl) {
        InputImpl input = (InputImpl) future;
        if (input.isPending()) {
          input.normalDisconnect(isNormalDisconnect);
        }
      }
      else {
        OutputImpl output = (OutputImpl) future;
        if (output.isPending()) {
          output.normalDisconnect(isNormalDisconnect);
        }
        output.pauseActionDone();
        output.resumeActionDone();
        output.speedActionDone();
        output.volumeActionDone();
      }
    }
  }
}

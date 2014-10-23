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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.media.mscontrol.EventType;
import javax.media.mscontrol.MediaErr;
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

import com.voxeo.moho.ApplicationContextImpl;
import com.voxeo.moho.MediaException;
import com.voxeo.moho.MediaService;
import com.voxeo.moho.Mixer;
import com.voxeo.moho.common.event.MohoInputCompleteEvent;
import com.voxeo.moho.common.event.MohoInputDetectedEvent;
import com.voxeo.moho.common.event.MohoOutputCompleteEvent;
import com.voxeo.moho.common.event.MohoOutputPausedEvent;
import com.voxeo.moho.common.event.MohoOutputResumedEvent;
import com.voxeo.moho.common.event.MohoRecordCompleteEvent;
import com.voxeo.moho.common.event.MohoRecordPausedEvent;
import com.voxeo.moho.common.event.MohoRecordResumedEvent;
import com.voxeo.moho.common.event.MohoRecordStartedEvent;
import com.voxeo.moho.common.util.InheritLogContextRunnable;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.event.MediaCompleteEvent;
import com.voxeo.moho.event.OutputCompleteEvent;
import com.voxeo.moho.event.OutputCompleteEvent.Cause;
import com.voxeo.moho.event.RecordCompleteEvent;
import com.voxeo.moho.media.dialect.CallRecordListener;
import com.voxeo.moho.media.dialect.MediaDialect;
import com.voxeo.moho.media.input.EnergyGrammar;
import com.voxeo.moho.media.input.Grammar;
import com.voxeo.moho.media.input.InputCommand;
import com.voxeo.moho.media.input.SignalGrammar;
import com.voxeo.moho.media.input.SignalGrammar.Signal;
import com.voxeo.moho.media.input.SimpleGrammar;
import com.voxeo.moho.media.output.AudibleResource;
import com.voxeo.moho.media.output.AudioURIResource;
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.moho.media.output.OutputCommand.BargeinType;
import com.voxeo.moho.media.output.TextToSpeechResource;
import com.voxeo.moho.media.record.RecordCommand;
import com.voxeo.moho.sip.JoinDelegate;
import com.voxeo.moho.sip.SIPCallImpl;
import com.voxeo.moho.spi.ExecutionContext;
import com.voxeo.moho.util.NLSMLParser;

public class GenericMediaService<T extends EventSource> implements MediaService<T> {

  private static final Logger LOG = Logger.getLogger(GenericMediaService.class);

  protected T _parent;

  protected MediaSession _session;

  protected MediaGroup _group;
  
  protected MediaGroup _group2;

  protected Player _player = null;

  protected Recorder _recorder = null;

  protected SignalDetector _detector = null;

  protected SignalGenerator _generator = null;

  protected ExecutionContext _context;

  protected MediaDialect _dialect;

  protected List<MediaOperation<?, ? extends MediaCompleteEvent<?>>> _futures = new CopyOnWriteArrayList<MediaOperation<?, ? extends MediaCompleteEvent<?>>>();

  protected PlayerListener playerListener = new PlayerListener();

  protected GenericMediaService(final T parent, final MediaGroup group) {
    _parent = parent;
    _group = group;
    _session = group.getMediaSession();
    _context = (ExecutionContext) ((EventSource) _parent).getApplicationContext();
    _dialect = ((ApplicationContextImpl) _context).getDialect();
  }

  protected synchronized Player getPlayer() {
    if (_player == null) {
      try {
        _player = _group.getPlayer();
        _player.addListener(playerListener);
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
      // For mixer, create another media group for recording, so the recorder
      // can record the output from the media service itself.
      if (_group2 == null && _parent instanceof Mixer) {
        try {
          _group2 = _session.createMediaGroup(MediaGroup.PLAYER_RECORDER_SIGNALDETECTOR_SIGNALGENERATOR, null);
        }
        catch (final MsControlException e1) {
          try {
            _group2 = _session.createMediaGroup(MediaGroup.PLAYER_RECORDER_SIGNALDETECTOR, null);
          }
          catch (final MsControlException e2) {
            try {
              _group2 = _session.createMediaGroup(MediaGroup.PLAYER, null);
            }
            catch (final MsControlException e3) {
              throw new MediaException(e3);
            }
          }
        }

        if (_group2 != null) {
          try {
            JoinDelegate.bridgeJoin((Mixer) _parent, _group2);
          }
          catch (Exception ex) {
            LOG.error("Exception when joining recorder media group", ex);
          }
        }
      }

      try {
        if (_group2 != null) {
          _recorder = _group2.getRecorder();
        }
        else {
          _recorder = _group.getRecorder();
        }
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

  @Override
  public MediaGroup getMediaGroup(boolean create) {
    return _group;
  }

  @Override
  public MediaGroup getMediaGroup() {
    return _group;
  }

  @Override
  public Input<T> input(final String grammar) throws MediaException {
    return prompt((String) null, grammar, 0).getInput();
  }

  @Override
  public Input<T> input(final InputCommand input) throws MediaException {
    return prompt(null, input, 0).getInput();
  }

  @Override
  public Output<T> output(final String text) throws MediaException {
    return prompt(text, null, 0).getOutput();
  }

  @Override
  public Output<T> output(final URI media) throws MediaException {
    return prompt(media, null, 0).getOutput();
  }

  @Override
  public Output<T> output(final OutputCommand output) throws MediaException {
    return prompt(output, null, 0).getOutput();
  }

  @Override
  public Prompt<T> prompt(final String text, final String grammar, final int repeat) throws MediaException {
    final OutputCommand output = text == null ? null : new OutputCommand(new TextToSpeechResource(text));
    final InputCommand input = grammar == null ? null : new InputCommand(new SimpleGrammar(grammar));
    return prompt(output, input, repeat);
  }

  @Override
  public Prompt<T> prompt(final URI media, final String grammar, final int repeat) throws MediaException {
    final OutputCommand output = media == null ? null : new OutputCommand(new AudioURIResource(media));
    final InputCommand input = grammar == null ? null : new InputCommand(new SimpleGrammar(grammar));
    return prompt(output, input, repeat);
  }

  private boolean haveOutput(OutputCommand output) {
    return output != null && output.getAudibleResources() != null && output.getAudibleResources().length > 0;
  }

  private boolean haveInput(InputCommand input) {
    return input != null && input.getGrammars() != null && input.getGrammars().length > 0;
  }

  private Prompt<T> internaOutput(OutputCommandInputCommandPair outinputPair) {
    OutputCommand output = outinputPair.getOutputCommand();
    int repeat = outinputPair.getRepeat();

    Prompt<T> retval = outinputPair.getPrompt();

    if (haveOutput(outinputPair.getOutputCommand())) {
      // _currentOutput = outinputPair;

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
      switch (output.getBargeinType()) {
        case ANY:
          rtcs.add(new RTC(SignalDetector.DETECTION_OF_ONE_SIGNAL, Player.STOP_ALL));
          rtcs.add(new RTC(SpeechDetectorConstants.START_OF_SPEECH, Player.STOP_ALL));
          params.put(SpeechDetectorConstants.BARGE_IN_ENABLED, Boolean.TRUE);
          break;
        case DTMF:
          rtcs.add(new RTC(SignalDetector.DETECTION_OF_ONE_SIGNAL, Player.STOP_ALL));
          params.put(SpeechDetectorConstants.BARGE_IN_ENABLED, Boolean.TRUE);
          break;
        case SPEECH:
          rtcs.add(new RTC(SpeechDetectorConstants.START_OF_SPEECH, Player.STOP_ALL));
          params.put(SpeechDetectorConstants.BARGE_IN_ENABLED, Boolean.TRUE);
          break;
        case NONE:
          params.put(SpeechDetectorConstants.BARGE_IN_ENABLED, Boolean.FALSE);
          break;
      }
      params.put(Player.MAX_DURATION, output.getMaxtime());
      params.put(Player.START_OFFSET, output.getStartingOffset());
      params.put(Player.VOLUME_CHANGE, output.getVolumeUnit());
      params.put(Player.AUDIO_CODEC, output.getCodec());
      params.put(Player.FILE_FORMAT, output.getFormat());
      params.put(Player.JUMP_PLAYLIST_INCREMENT, output.getJumpPlaylistIncrement());
      params.put(Player.JUMP_TIME, output.getMoveTime());
      params.put(Player.START_IN_PAUSED_MODE, output.isStartInPausedMode());

      params.put(Player.ENABLED_EVENTS, new EventType[] {PlayerEvent.SPEED_CHANGED, PlayerEvent.VOLUME_CHANGED,
          PlayerEvent.RESUMED, PlayerEvent.PAUSED});

      _dialect.setTextToSpeechVoice(params, output.getVoiceName());
      _dialect.setTextToSpeechLanguage(params, output.getLanguage());

      if (output.getRepeatTimes() > 1) {
        params.put(Player.REPEAT_COUNT, output.getRepeatTimes());
        params.put(Player.INTERVAL, output.getRepeatInterval());
      }

      if (repeat > 1) {
        params.put(Player.REPEAT_COUNT, repeat);
        params.put(Player.INTERVAL, output.getRepeatInterval());
      }

      final List<URI> uris = new ArrayList<URI>();
      final MediaResource[] reses = output.getAudibleResources();
      for (final MediaResource r : reses) {
        uris.add(r.toURI());
      }

      if (haveInput(outinputPair.getInputCommand())) {
        params.put(SignalDetector.PROMPT, uris.toArray(new URI[] {}));
        detectSignal(outinputPair.getInputCommand(), (InputImpl<T>) retval.getInput(), params, rtcs);
      }
      else {
        try {
          playerListener.addOutput(outinputPair.getOutput());
          getPlayer().play(uris.toArray(new URI[] {}), rtcs.toArray(new RTC[] {}), params);
          _futures.add(outinputPair.getOutput());
        }
        catch (final MsControlException e) {
          playerListener.removeOutput(outinputPair.getOutput());
          throw new MediaException(e);
        }
      }
    }
    else {
      detectSignal(outinputPair.getInputCommand(), (InputImpl<T>) retval.getInput(), null, null);
    }
    return retval;
  }

  @SuppressWarnings("deprecation")
  @Override
  public synchronized Prompt<T> prompt(final OutputCommand output, final InputCommand input, final int repeat)
      throws MediaException {
    if (!haveInput(input) && !haveOutput(output)) {
      throw new IllegalArgumentException("No output or input.");
    }
    final PromptImpl<T> retval = new PromptImpl<T>();
    OutputImpl<T> outFuture = null;
    InputImpl<T> inFuture = null;
    if (haveInput(input)) {
      inFuture = new InputImpl<T>(_group);
      retval.setInput(inFuture);
    }
    else if (output != null) {
      outFuture = new OutputImpl<T>(_group);
      retval.setOutput(outFuture);
    }
    OutputCommandInputCommandPair outinputPair = new OutputCommandInputCommandPair(output, input, inFuture, outFuture,
        repeat, retval);

    return internaOutput(outinputPair);
  }

  @Override
  public Recording<T> record(final URI recording) throws MediaException {
    Recorder recorder = getRecorder();
    final RecordingImpl<T> retval = new RecordingImpl<T>(_group2 != null? _group2 : _group);
    try {
      recorder.addListener(new RecorderListener(retval));
      recorder.record(recording, RTC.NO_RTC, Parameters.NO_PARAMETER);
      _futures.add(retval);
      return retval;
    }
    catch (final Exception e) {
      throw new MediaException(e);
    }
  }

  @Override
  public Recording<T> record(final RecordCommand command) throws MediaException {
    if (command.isDuplex() && _parent instanceof SIPCallImpl) {
      NetworkConnection nc = (NetworkConnection) ((SIPCallImpl) _parent).getMediaObject();
      final CallRecordingImpl<T> retValue = new CallRecordingImpl<T>(nc, _dialect);
      try {
        final Parameters params = _group.createParameters();
        if (command.getAudioCODEC() != null) {
          _dialect.setCallRecordAudioCodec(params, command.getAudioCODEC());
        }
        if (command.getFileFormat() != null) {
          _dialect.setCallRecordFileFormat(params, command.getFileFormat());
        }
        
        _dialect.startCallRecord(nc, command.getRecordURI(), RTC.NO_RTC, params, new CallRecordListenerImpl(retValue, command));
        
        if(command.getMaxDuration() > 0) {
          MaxCallRecordDurationTask timerTask = new MaxCallRecordDurationTask(retValue);
          ScheduledFuture future = ((ApplicationContextImpl) _context).getScheduledEcutor().schedule(timerTask, command.getMaxDuration(), TimeUnit.MILLISECONDS);
          retValue.setMaxDurationTimerFuture(future);
          retValue.setMaxDurationTask(timerTask);
        }
        _futures.add(retValue);
      }
      catch (Exception ex) {
        throw new MediaException(ex);
      }

      return retValue;
    }
    Recorder recorder = getRecorder();
    final RecordingImpl<T> retval = new RecordingImpl<T>(_group2 != null? _group2 : _group);
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
        if (command.getBeepFrequency() > 0 || command.getBeepFrequency() == -1) {
          params.put(Recorder.BEEP_FREQUENCY, command.getBeepFrequency());
        }
        if (command.getBeepLength() > 0 || command.getBeepLength() == -1) {
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

        if (command.getPrompt().getBargeinType() != BargeinType.NONE) {
          params.put(SpeechDetectorConstants.BARGE_IN_ENABLED, Boolean.TRUE);
          params.put(Recorder.SPEECH_DETECTION_MODE, Recorder.DETECT_FIRST_OCCURRENCE);
        }

        if (command.getPrompt().getVoiceName() != null) {
          _dialect.setTextToSpeechVoice(params, command.getPrompt().getVoiceName());
        }
        if (command.getPrompt().getLanguage() != null) {
          _dialect.setTextToSpeechLanguage(params, command.getPrompt().getLanguage());
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

      params.put(SpeechDetectorConstants.INITIAL_TIMEOUT, command.getInitialTimeout());
      if (command.getFinalTimeout() > 0) {
        params.put(Recorder.SILENCE_TERMINATION_ON, Boolean.TRUE);
        params.put(SpeechDetectorConstants.FINAL_TIMEOUT, command.getFinalTimeout());
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

      _dialect.enableRecorderPromptCompleteEvent(params, true);

      if (command.isIgnorePromptFailure()) {
        _dialect.setIgnorePromptFailure(params, true);
      }

      recorder.addListener(new RecorderListener(retval));
      recorder.record(command.getRecordURI(), rtcs.toArray(new RTC[] {}), params);
      _futures.add(retval);
      return retval;
    }
    catch (final Exception e) {
      throw new MediaException(e);
    }
  }

  @SuppressWarnings("deprecation")
  protected Input<T> detectSignal(final InputCommand cmd, final InputImpl<T> input, Parameters internalParams,
      List<RTC> internalRtcs) throws MediaException {
    if (cmd == null) {
      throw new MediaException("InputCommand not initialized");
    }
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

    if (internalParams != null) {
      params.putAll(internalParams);
    }

    final List<RTC> rtcs = new ArrayList<RTC>();
    if (cmd.getRtcs() != null) {
      for (final RTC rtc : cmd.getRtcs()) {
        rtcs.add(rtc);
      }
    }

    if (internalRtcs != null) {
      rtcs.addAll(internalRtcs);
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
    params.put(SignalDetector.INTER_SIG_TIMEOUT, cmd.getInterDigitsTimeout());
    params.put(SpeechDetectorConstants.SENSITIVITY, cmd.getSensitivity());

    if (cmd.isSupervised()) {
      _dialect.enableDetectorPromptCompleteEvent(params, true);

      final EventType[] enabledEvent = (EventType[]) params.get(SignalDetector.ENABLED_EVENTS);
      if (enabledEvent != null && enabledEvent.length > 0) {
        EventType[] newEabledEvents = Arrays.copyOf(enabledEvent, enabledEvent.length + 1);
        newEabledEvents[newEabledEvents.length - 1] = SignalDetectorEvent.SIGNAL_DETECTED;
        params.put(SignalDetector.ENABLED_EVENTS, newEabledEvents);
      }
      else {
        params.put(SignalDetector.ENABLED_EVENTS, new EventType[] {SignalDetectorEvent.SIGNAL_DETECTED});
      }
    }

    if (cmd.getSpeechCompleteTimeout() > 0) {
      _dialect.setSpeechCompleteTimeout(params, cmd.getSpeechCompleteTimeout());
    }

    if (cmd.getSpeechIncompleteTimeout() > 0) {
      _dialect.setSpeechIncompleteTimeout(params, cmd.getSpeechIncompleteTimeout());
    }

    if (cmd.isIgnorePromptFailure()) {
      _dialect.setIgnorePromptFailure(params, true);
    }

    _dialect.setSpeechLanguage(params, cmd.getRecognizer());
    _dialect.setSpeechTermChar(params, cmd.getTerminator());
    _dialect.setSpeechInputMode(params, cmd.getInputMode());
    _dialect.setDtmfHotwordEnabled(params, cmd.isDtmfHotword());
    _dialect.setDtmfTypeaheadEnabled(params, cmd.isDtmfTypeahead());
    _dialect.setConfidence(params, cmd.getMinConfidence());
    _dialect.setAutoReset(params, cmd.getAutoReset());
    _dialect.setEnergyParameters(params, cmd.getEnergyParameters());
    _dialect.setBeepParameters(params, cmd.getBeepParameters());

    final Grammar[] grammars = cmd.getGrammars();
    List<InputPattern> patterns = null;
    List<Parameter> patternKeys = null;
    List<EventType> patternMatchedEvts = null;
    if (grammars.length > 0) {
      patterns = new ArrayList<InputPattern>(grammars.length);
      patternKeys = new ArrayList<Parameter>(grammars.length);
      patternMatchedEvts = new ArrayList<EventType>(grammars.length);

      int i = 0;
      for (final Grammar grammar : grammars) {
        if (grammar == null) {
          continue;
        }

        Object pattern = null;
        if (grammar instanceof EnergyGrammar) {
          final EnergyGrammar eg = (EnergyGrammar) grammar;
          if (eg.isStartOfSpeech()) {
            pattern = SpeechRecognitionEvent.START_OF_SPEECH;
          }
          else if (eg.isEndOfSpeech()) {
            pattern = SpeechRecognitionEvent.END_OF_SPEECH;
          }
        }
        else if (grammar instanceof SignalGrammar) {
          final SignalGrammar sg = (SignalGrammar) grammar;
          pattern = _dialect.getSignalConstants(sg.getSignal());
        }
        else {
          final URI uri = grammar.toURI();

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
        }

        if (pattern == null) {
          continue;
        }

        patterns.add(new InputPattern(i, pattern, grammar.isTerminatingCondition()));
        if (grammar.isTerminatingCondition()) {
          patternKeys.add(SignalDetector.PATTERN[i]);
        }
        else {
          patternMatchedEvts.add(SignalDetectorEvent.PATTERN_MATCHED[i]);
        }

        i++;
      }

      if (patterns.size() > 0) {
        final Parameters patternParams = _group.createParameters();
        for (InputPattern p : patterns) {
          if (LOG.isDebugEnabled()) {
            LOG.debug(p);
          }
          patternParams.put(SignalDetector.PATTERN[p.getIndex()], p.getValue());
        }
        _group.setParameters(patternParams);
      }

      if (patternMatchedEvts.size() > 0) {
        final EventType[] enabledEvent = (EventType[]) params.get(SignalDetector.ENABLED_EVENTS);
        if (enabledEvent != null && enabledEvent.length > 0) {
          EventType[] newEabledEvents = Arrays.copyOf(enabledEvent, enabledEvent.length + patternMatchedEvts.size());
          System.arraycopy(patternMatchedEvts.toArray(new EventType[patternMatchedEvts.size()]), 0, newEabledEvents,
              enabledEvent.length, patternMatchedEvts.size());
          params.put(SignalDetector.ENABLED_EVENTS, newEabledEvents);
        }
        else {
          params.put(SignalDetector.ENABLED_EVENTS,
              patternMatchedEvts.toArray(new EventType[patternMatchedEvts.size()]));
        }
      }
    }

    if (patterns == null && cmd.getNumberOfDigits() == -1) {
      throw new MediaException("No pattern");
    }

    getSignalDetector().addListener(new DetectorListener(input, cmd, patterns));
    try {
      if (cmd.isFlushBuffer()) {
        getSignalDetector().flushBuffer();
      }
      getSignalDetector().receiveSignals(
          cmd.getNumberOfDigits(),
          (patternKeys == null || patternKeys.size() == 0) ? SignalDetector.NO_PATTERN : patternKeys
              .toArray(new Parameter[patternKeys.size()]), rtcs.toArray(new RTC[] {}), params);
      _futures.add(input);
    }
    catch (final MsControlException e) {
      // if (params.get(SignalDetector.PROMPT) != null) {
      // _currentOutput = null;
      // }
      throw new MediaException(e);
    }
    return input;
  }

  protected class CallRecordListenerImpl implements CallRecordListener {
    private RecordCommand command;

    private CallRecordingImpl<T> callRecording;
    
    private long duration = 0;

    public CallRecordListenerImpl(CallRecordingImpl<T> callRecording, RecordCommand command) {
      super();
      this.callRecording = callRecording;
      this.command = command;
    }

    @Override
    public void callRecordComplete(ResourceEvent event) {
      Qualifier q = event.getQualifier();
      RecordCompleteEvent.Cause cause = RecordCompleteEvent.Cause.UNKNOWN;
      String errorText = null;
      if (q == SpeechDetectorConstants.INITIAL_TIMEOUT_EXPIRED) {
        cause = RecordCompleteEvent.Cause.INI_TIMEOUT;
      }
      else if (q == ResourceEvent.STOPPED) {
        if (callRecording.isMaxDurationStop()) {
          cause = RecordCompleteEvent.Cause.TIMEOUT;
        }
        else if (callRecording.isNormalDisconnect()) {
          cause = RecordCompleteEvent.Cause.DISCONNECT;
        }
        else {
          cause = RecordCompleteEvent.Cause.CANCEL;
        }
      }
      else if (q == ResourceEvent.RTC_TRIGGERED) {
        cause = RecordCompleteEvent.Cause.CANCEL;
      }
      else if (q == ResourceEvent.NO_QUALIFIER) {
        if (event.getError() != ResourceEvent.NO_ERROR) {
          cause = RecordCompleteEvent.Cause.ERROR;
        }

        errorText = event.getError() + ": " + event.getErrorText();
      }
      duration = _dialect.getCallRecordDuration(event);
      final RecordCompleteEvent<T> recordCompleteEvent = new MohoRecordCompleteEvent<T>(_parent, cause, duration,
          errorText, callRecording);
      _parent.dispatch(recordCompleteEvent);
      callRecording.done(recordCompleteEvent);
      _futures.remove(callRecording);
      if (callRecording.getMaxDurationTimerFuture() != null) {
        callRecording.getMaxDurationTimerFuture().cancel(true);
        ((ApplicationContextImpl) _context).getScheduledEcutor().remove(callRecording.getMaxDurationTask());
        ((ApplicationContextImpl) _context).getScheduledEcutor().purge();
      }
    }

    @Override
    public void callRecordPause(ResourceEvent event) {
      duration=_dialect.getCallRecordDuration(event);
      _parent.dispatch(new MohoRecordPausedEvent<T>(_parent));
      callRecording.pauseActionDone();
      if (callRecording.getMaxDurationTimerFuture() != null) {
        // cancel max-duration task
        callRecording.getMaxDurationTimerFuture().cancel(true);
        ((ApplicationContextImpl) _context).getScheduledEcutor().remove(callRecording.getMaxDurationTask());
        ((ApplicationContextImpl) _context).getScheduledEcutor().purge();
        callRecording.setMaxDurationTimerFuture(null);
        callRecording.setMaxDurationTask(null);
      }
    }

    @Override
    public void callRecordResume(ResourceEvent event) {
      if (!event.isSuccessful()) {
        final RecordCompleteEvent<T> recordCompleteEvent = new MohoRecordCompleteEvent<T>(_parent,
            RecordCompleteEvent.Cause.ERROR, _dialect.getCallRecordDuration(event), callRecording);
        _parent.dispatch(recordCompleteEvent);
        callRecording.done(new MediaException(event.getErrorText()));
        _futures.remove(callRecording);
      }
      else {
        _parent.dispatch(new MohoRecordResumedEvent<T>(_parent));
        callRecording.resumeActionDone();
        if (command.getMaxDuration() > 0) {
          // restart max-duration task
          long delay = command.getMaxDuration() - duration;
          MaxCallRecordDurationTask timerTask = new MaxCallRecordDurationTask(callRecording);
          ScheduledFuture future = ((ApplicationContextImpl) _context).getScheduledEcutor().schedule(timerTask, delay,
              TimeUnit.MILLISECONDS);
          callRecording.setMaxDurationTimerFuture(future);
          callRecording.setMaxDurationTask(timerTask);
        }
      }
    }
  }

  @SuppressWarnings("rawtypes")
  protected class PlayerListener implements MediaEventListener<PlayerEvent> {
    Queue<OutputImpl> _outputQueue = new LinkedList<OutputImpl>();

    public void addOutput(OutputImpl outputImpl) {
      _outputQueue.offer(outputImpl);
    }

    public void removeOutput(OutputImpl outputImpl) {
      _outputQueue.remove(outputImpl);
    }

    @Override
    public void onEvent(final PlayerEvent e) {
      Runnable runable = new InheritLogContextRunnable() {
        @Override
        public void run() {
          final EventType t = e.getEventType();
          synchronized (GenericMediaService.this) {
            OutputImpl _output = _outputQueue.peek();
            if (_output == null) {
              LOG.error("Received PlayerEvent, but didn't find corresponding output future. " + e);
              throw new RuntimeException("Received PlayerEvent, but didn't find corresponding output future.");
            }
            if (t == PlayerEvent.PLAY_COMPLETED) {
              _outputQueue.remove(_output);
              OutputCompleteEvent.Cause cause = Cause.UNKNOWN;
              String errorText = null;
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
                  if (_output.isNormalDisconnect()) {
                    cause = Cause.DISCONNECT;
                  }
                  else {
                    cause = Cause.CANCEL;
                  }
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
              else if (q == ResourceEvent.NO_QUALIFIER) {
                if (e.getError() != ResourceEvent.NO_ERROR) {
                  cause = Cause.ERROR;
                }

                errorText = e.getError() + ": " + e.getErrorText();
              }
              final OutputCompleteEvent<T> outputCompleteEvent = new MohoOutputCompleteEvent<T>(_parent, cause,
                  errorText, _output);

              _output.done(outputCompleteEvent);
              _parent.dispatch(outputCompleteEvent);

              _futures.remove(_output);
            }
            else if (t == PlayerEvent.PAUSED) {
              _output.pauseActionDone();
              _parent.dispatch(new MohoOutputPausedEvent<T>(_parent));
            }
            else if (t == PlayerEvent.RESUMED) {
              _output.resumeActionDone();
              _parent.dispatch(new MohoOutputResumedEvent<T>(_parent));
            }
            else if (t == PlayerEvent.SPEED_CHANGED) {
              _output.speedActionDone();
            }
            else if (t == PlayerEvent.VOLUME_CHANGED) {
              _output.volumeActionDone();
            }
          }
        }
      };
      // avoid deadlock with 309 trigger
      if (_context != null) {
        _context.getExecutor().execute(runable);
      }
      else {
        runable.run();
      }
    }
  }

  protected class DetectorListener implements MediaEventListener<SignalDetectorEvent> {

    private InputImpl<T> _input = null;

    private InputCommand _cmd = null;

    private List<InputPattern> _patterns = null;

    public DetectorListener(final InputImpl<T> input, final InputCommand inputCmd, final List<InputPattern> patterns) {
      _input = input;
      _cmd = inputCmd;
      _patterns = patterns;
    }

    @Override
    public void onEvent(final SignalDetectorEvent e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("DetectorListener[" + _parent + "] received: " + e);
      }
      final EventType t = e.getEventType();
      if (t == SignalDetectorEvent.RECEIVE_SIGNALS_COMPLETED) {
        getSignalDetector().removeListener(this);
        if (_cmd.isRecord()) {
          getRecorder().stop();
        }
        InputCompleteEvent.Cause cause = InputCompleteEvent.Cause.UNKNOWN;
        final Qualifier q = e.getQualifier();
        String errorText = null;
        final InputPattern pattern = patternMatched(q);
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
        else if (q == ResourceEvent.STOPPED || q == ResourceEvent.RTC_TRIGGERED) {
          if (_input.isNormalDisconnect()) {
            cause = InputCompleteEvent.Cause.DISCONNECT;
          }
          else {
            cause = InputCompleteEvent.Cause.CANCEL;
          }
        }
        else if (q == SignalDetectorEvent.NUM_SIGNALS_DETECTED) {
          cause = InputCompleteEvent.Cause.MATCH;
        }
        else if (pattern != null) {
          if (pattern.getValue() == SpeechRecognitionEvent.START_OF_SPEECH) {
            cause = InputCompleteEvent.Cause.START_OF_SPEECH;
          }
          else if (pattern.getValue() == SpeechRecognitionEvent.END_OF_SPEECH) {
            cause = InputCompleteEvent.Cause.END_OF_SPEECH;
          }
          else {
            cause = InputCompleteEvent.Cause.MATCH;
          }
        }
        else if (q == ResourceEvent.NO_QUALIFIER) {
          if (e.getError() != ResourceEvent.NO_ERROR) {
            cause = InputCompleteEvent.Cause.ERROR;
          }

          errorText = e.getError() + ": " + e.getErrorText();
        }
        else {
          cause = _dialect.getInputCompleteEventCause(q);
        }
        final MohoInputCompleteEvent<T> inputCompleteEvent = new MohoInputCompleteEvent<T>(_parent,
            cause == null ? InputCompleteEvent.Cause.UNKNOWN : cause, errorText, _input);
        if (e instanceof SpeechRecognitionEvent) {
          final SpeechRecognitionEvent se = (SpeechRecognitionEvent) e;
          String signalString = e.getSignalString();
          if (signalString != null) {
            final Signal signal = Signal.parse(signalString);
            if (signal == null) {
              inputCompleteEvent.setConcept(se.getTag());
              inputCompleteEvent.setTag(se.getTag());
              inputCompleteEvent.setConfidence(1.0F);
              inputCompleteEvent.setInterpretation(signalString);
              inputCompleteEvent.setUtterance(signalString);
              inputCompleteEvent.setInputMode(InputMode.DTMF);
            }
            else {
              inputCompleteEvent.setSignal(signal);
            }
          }
          else {
            inputCompleteEvent.setUtterance(se.getUserInput());
            inputCompleteEvent.setTag(se.getTag());
            inputCompleteEvent.setConcept(se.getTag());
          }

          final URL semanticResult = se.getSemanticResult();
          if (semanticResult != null && "application/x-nlsml".equalsIgnoreCase(semanticResult.getHost())) {
            try {
              inputCompleteEvent.setNlsml(semanticResult.getPath());
              final List<Map<String, String>> nlsml = NLSMLParser.parse(inputCompleteEvent.getNlsml());
              for (final Map<String, String> reco : nlsml) {
                final String conf = reco.get("_confidence");
                if (conf != null && signalString == null) {
                  inputCompleteEvent.setConfidence(Float.parseFloat(conf));
                }
                final String interpretation = reco.get("_interpretation");
                if (interpretation != null) {
                  inputCompleteEvent.setInterpretation(interpretation);
                }
                final String inputmode = reco.get("_inputmode");
                if (inputmode != null && signalString == null) {
                  if (inputmode.equalsIgnoreCase("speech") || inputmode.equalsIgnoreCase("voice")) {
                    inputCompleteEvent.setInputMode(InputMode.SPEECH);
                  }
                  else {
                    inputCompleteEvent.setInputMode(InputMode.DTMF);
                  }
                }
              }
            }
            catch (final Exception e1) {
              LOG.warn("No NLSML", e1);
            }
          }
        }
        else {
          String signalString = e.getSignalString();
          final Signal signal = Signal.parse(signalString);
          if (signal == null) {
            inputCompleteEvent.setConcept(signalString);
            inputCompleteEvent.setConfidence(1.0F);
            inputCompleteEvent.setInterpretation(signalString);
            inputCompleteEvent.setUtterance(signalString);
            inputCompleteEvent.setInputMode(InputMode.DTMF);
          }
          else {
            inputCompleteEvent.setSignal(signal);
          }
        }
        inputCompleteEvent.setSISlots(_dialect.getSISlots(e));

        _parent.dispatch(inputCompleteEvent);

        _input.done(inputCompleteEvent);
        _futures.remove(_input);
      }
      else if (t == SignalDetectorEvent.SIGNAL_DETECTED) {
        if (_cmd.isSupervised()) {
          _parent.dispatch(new MohoInputDetectedEvent<T>(_parent, e.getSignalString()));
        }
      }
      else if (_dialect.isPromptCompleteEvent(e)) {
        _parent.dispatch(new MohoOutputCompleteEvent<T>(_parent, OutputCompleteEvent.Cause.END, _input));
      }
      else {
        final InputPattern pattern = patternMatched(t);
        if (pattern == null) {
          LOG.warn("Skipped " + e.getEventType() + " event! No InputPattern is found.");
          return;
        }

        final MohoInputDetectedEvent<T> inputDetectedEvent;
        if (pattern.getValue() == SpeechRecognitionEvent.START_OF_SPEECH) {
          inputDetectedEvent = new MohoInputDetectedEvent<T>(_parent, true, false);
        }
        else if (pattern.getValue() == SpeechRecognitionEvent.END_OF_SPEECH) {
          inputDetectedEvent = new MohoInputDetectedEvent<T>(_parent, false, true);
        }
        else {
          Signal signal = Signal.parse(e.getSignalString());
          if (signal != null) {
            inputDetectedEvent = new MohoInputDetectedEvent<T>(_parent, signal);
          }
          else {
            String input = null;
            if (e instanceof SpeechRecognitionEvent) {
              input = ((SpeechRecognitionEvent) e).getUserInput();
              if (input == null) {
                input = e.getSignalString();
              }
            }
            else {
              input = e.getSignalString();
            }
            inputDetectedEvent = new MohoInputDetectedEvent<T>(_parent, input);
            if (e instanceof SpeechRecognitionEvent) {
              final SpeechRecognitionEvent se = (SpeechRecognitionEvent) e;
              String signalString = e.getSignalString();
              if (signalString != null) {
                inputDetectedEvent.setConcept(se.getTag());
                inputDetectedEvent.setTag(se.getTag());
                inputDetectedEvent.setConfidence(1.0F);
                inputDetectedEvent.setInterpretation(signalString);
                inputDetectedEvent.setInputMode(InputMode.DTMF);
              }
              else {
                inputDetectedEvent.setTag(se.getTag());
                inputDetectedEvent.setConcept(se.getTag());
              }

              final URL semanticResult = se.getSemanticResult();
              if (semanticResult != null && "application/x-nlsml".equalsIgnoreCase(semanticResult.getHost())) {
                try {
                  inputDetectedEvent.setNlsml(semanticResult.getPath());
                  final List<Map<String, String>> nlsml = NLSMLParser.parse(inputDetectedEvent.getNlsml());
                  for (final Map<String, String> reco : nlsml) {
                    final String conf = reco.get("_confidence");
                    if (conf != null && signalString == null) {
                      inputDetectedEvent.setConfidence(Float.parseFloat(conf));
                    }
                    final String interpretation = reco.get("_interpretation");
                    if (interpretation != null) {
                      inputDetectedEvent.setInterpretation(interpretation);
                    }
                    final String inputmode = reco.get("_inputmode");
                    if (inputmode != null && signalString == null) {
                      if (inputmode.equalsIgnoreCase("speech") || inputmode.equalsIgnoreCase("voice")) {
                        inputDetectedEvent.setInputMode(InputMode.SPEECH);
                      }
                      else {
                        inputDetectedEvent.setInputMode(InputMode.DTMF);
                      }
                    }
                  }
                }
                catch (final Exception e1) {
                  LOG.warn("No NLSML", e1);
                }
              }
            }
            else {
              String signalString = e.getSignalString();
              inputDetectedEvent.setConcept(signalString);
              inputDetectedEvent.setConfidence(1.0F);
              inputDetectedEvent.setInterpretation(signalString);
              inputDetectedEvent.setInputMode(InputMode.DTMF);
            }
            inputDetectedEvent.setSISlots(_dialect.getSISlots(e));
          }
        }
        _parent.dispatch(inputDetectedEvent);
      }
    }

    private InputPattern patternMatched(final Qualifier qualifier) {
      if (_patterns != null && _patterns.size() > 0) {
        for (final InputPattern p : _patterns) {
          if (qualifier == SignalDetectorEvent.PATTERN_MATCHING[p.getIndex()]) {
            return p;
          }
        }
      }
      return null;
    }

    private InputPattern patternMatched(final EventType type) {
      if (_patterns != null && _patterns.size() > 0) {
        for (final InputPattern p : _patterns) {
          if (type == SignalDetectorEvent.PATTERN_MATCHED[p.getIndex()]) {
            return p;
          }
        }
      }
      return null;
    }
  }

  protected class RecorderListener implements MediaEventListener<RecorderEvent> {

    private RecordingImpl<T> _recording = null;

    public RecorderListener(final RecordingImpl<T> recording) {
      _recording = recording;
    }

    @Override
    public void onEvent(final RecorderEvent e) {
      final EventType t = e.getEventType();
      if (t == RecorderEvent.RECORD_COMPLETED) {
        getRecorder().removeListener(this);
        RecordCompleteEvent.Cause cause = RecordCompleteEvent.Cause.UNKNOWN;
        String errorText = null;
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
        else if (q == ResourceEvent.NO_QUALIFIER) {
          if (e.getError() != ResourceEvent.NO_ERROR) {
            cause = RecordCompleteEvent.Cause.ERROR;
          }

          errorText = e.getError() + ": " + e.getErrorText();
        }
        final RecordCompleteEvent<T> recordCompleteEvent = new MohoRecordCompleteEvent<T>(_parent, cause,
            e.getDuration(), errorText, _recording);
        _parent.dispatch(recordCompleteEvent);
        _recording.done(recordCompleteEvent);
        _futures.remove(_recording);
      }
      else if (t == RecorderEvent.PAUSED) {
        _parent.dispatch(new MohoRecordPausedEvent<T>(_parent));
        _recording.pauseActionDone();
      }
      else if (t == RecorderEvent.RESUMED) {
        if (e.getError() == MediaErr.UNKNOWN_ERROR) {
          final RecordCompleteEvent<T> recordCompleteEvent = new MohoRecordCompleteEvent<T>(_parent,
              RecordCompleteEvent.Cause.ERROR, e.getDuration(), _recording);
          _parent.dispatch(recordCompleteEvent);
          _recording.done(new MediaException(e.getErrorText()));
          _futures.remove(_recording);
        }
        else {
          _parent.dispatch(new MohoRecordResumedEvent<T>(_parent));
          _recording.resumeActionDone();
        }
      }
      else if (t == RecorderEvent.STARTED) {
        _parent.dispatch(new MohoRecordStartedEvent<T>(_parent));
      }
      else if (_dialect.isPromptCompleteEvent(e)) {
        _parent.dispatch(new MohoOutputCompleteEvent<T>(_parent, OutputCompleteEvent.Cause.END, _recording));
      }
    }
  }

  @SuppressWarnings("unchecked")
  public void release(boolean isNormalDisconnect) {
    // avoid ConcurrentModificationException
    List<MediaOperation<?, ? extends MediaCompleteEvent<?>>> copy = new LinkedList<MediaOperation<?, ? extends MediaCompleteEvent<?>>>();
    copy.addAll(_futures);
    Iterator<MediaOperation<? extends EventSource, ? extends MediaCompleteEvent<?>>> ite = copy.iterator();

    while (ite.hasNext()) {
      MediaOperation<? extends EventSource, ? extends MediaCompleteEvent<?>> future = ite.next();

      if (future instanceof RecordingImpl) {
        RecordingImpl<T> recording = (RecordingImpl<T>) future;
        recording.pauseActionDone();
        recording.resumeActionDone();

        if (recording.isPending()) {
          recording.normalDisconnect(isNormalDisconnect);
          try {
            recording.stop();
          }
          catch (Exception e) {
            LOG.warn("Exception when stopping record.", e);
          }
        }
      }
      else if (future instanceof InputImpl) {
        InputImpl<T> input = (InputImpl<T>) future;
        if (input.isPending()) {
          input.normalDisconnect(isNormalDisconnect);
          try {
            input.stop();
          }
          catch (Exception e) {
            LOG.warn("Exception when stopping input.", e);
          }
        }
      }
      else if (future instanceof CallRecordingImpl) {
        CallRecordingImpl<T> recording = (CallRecordingImpl<T>) future;
        if (!recording.isDone()) {
          recording.normalDisconnect(isNormalDisconnect);
          try {
            recording.stop();
          }
          catch (Exception e) {
            LOG.warn("Exception when stopping call record.", e);
          }
        }
      }
      else {
        OutputImpl<T> output = (OutputImpl<T>) future;
        output.pauseActionDone();
        output.resumeActionDone();
        output.speedActionDone();
        output.volumeActionDone();

        if (output.isPending()) {
          output.normalDisconnect(isNormalDisconnect);

          try {
            output.stop();
          }
          catch (Exception e) {
            LOG.warn("Exception when stopping output.", e);
          }
        }
      }
    }
  }

  class OutputCommandInputCommandPair {
    private OutputCommand _outputCommand;

    private InputCommand _inputCommand;

    private int _repeat;

    private InputImpl<T> _input;

    private OutputImpl<T> _output;

    private PromptImpl<T> _prompt;

    public OutputCommandInputCommandPair(OutputCommand outputCommand, InputCommand inputCommand, InputImpl<T> input,
        OutputImpl<T> output, int repeat, PromptImpl<T> prompt) {
      super();
      this._outputCommand = outputCommand;
      this._inputCommand = inputCommand;
      this._input = input;
      this._output = output;
      this._repeat = repeat;
      this._prompt = prompt;
    }

    public PromptImpl<T> getPrompt() {
      return _prompt;
    }

    public InputImpl<T> getInput() {
      return _input;
    }

    public OutputImpl<T> getOutput() {
      return _output;
    }

    public OutputCommand getOutputCommand() {
      return _outputCommand;
    }

    public InputCommand getInputCommand() {
      return _inputCommand;
    }

    public int getRepeat() {
      return _repeat;
    }
  }

  public class MaxCallRecordDurationTask extends InheritLogContextRunnable {
    private CallRecordingImpl<T> future;
    
    public MaxCallRecordDurationTask(CallRecordingImpl<T> future) {
      super();
      this.future = future;
    }

    @Override
    public void run() {
      LOG.warn("Max call record duration expired, stopping.");
      future.setMaxDurationStop(true);
      future.stop();
      future.setMaxDurationTimerFuture(null);
    }
  }
}
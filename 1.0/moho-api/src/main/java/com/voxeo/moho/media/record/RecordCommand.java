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

package com.voxeo.moho.media.record;

import java.net.URI;
import java.net.URL;

import javax.media.mscontrol.Value;

public class RecordCommand {

  public enum SpeechDetectionMode {
    DETECTOR_INACTIVE, DETECT_FIRST_OCCURRENCE, DETECT_ALL_OCCURRENCES
  }

  protected URL _recordURL = null;

  protected boolean bargine;// SIGDET_STOPRECORD

  /**
   * APPEND Indicates that recording should append to the end of an existing TVM
   * rather than overwrite it. Default: Boolean.FALSE.
   */
  protected boolean _append;

  /**
   * AUDIO_CLOCKRATE The desired clock rate (or sample rate) of the audio
   * encoding. The value is an Integer, in Hz. Default 8000.
   */
  protected int _audioClockRate;

  /**
   * AUDIO_CODEC Parameter identifying the audio Codec used for a new recording.
   * Default: CodecConstants.INFERRED.
   */
  protected Value _audioCODEC;

  /**
   * AUDIO_FMTP A string-valued list of detailed audio codec parameters, in the
   * format described by rfc4566. for example "bitrate=5.3;annexa=no". Default
   * empty string.
   */
  protected String _audioFMTP;

  /**
   * AUDIO_MAX_BITRATE The maximum accepted bitrate for the audio stream, in
   * bits-per-second units.
   */
  protected int _audioMaxBitRate;

  /**
   * BEEP_FREQUENCY The frequency of the start beep. Default: 400.
   */
  protected int _beepFrequency;

  /**
   * BEEP_LENGTH Length of Beep preceeding recording. Default: 125.
   */
  protected int _beepLength;

  /**
   * FILE_FORMAT A Parameter identifying the File Format used during recording.
   * Default FileFormatConstants.INFERRED.
   */
  protected Value _fileFormat;

  /**
   * MAX_DURATION Positive Integer indicating the maximum duration (in
   * milliseconds) for a record. Default: Resource.FOR_EVER
   */
  protected int _maxDuration;

  /**
   * MIN_DURATION Integer indicating minimum duration (in milliseconds) that
   * constitutes a valid recording. Default: 0
   */
  protected int _minDuration;

  /**
   * PROMPT Indicates a prompt to be played before the recording starts.
   */
  protected URI[] _prompt;

  /**
   * SIGNAL_TRUNCATION_ON Boolean indicating whether signal(DTMF) truncation is
   * enabled.Default: Boolean.TRUE
   */
  protected boolean _signalTruncationOn;

  /**
   * SILENCE_TERMINATION_ON Boolean indicating if a silence will terminate the
   * recording.Default: Boolean.FALSE
   */
  protected boolean _silenceTerminationOn;

  /**
   * SPEECH_DETECTION_MODE A Parameter identifying the Speech detection mode.
   * Default: Recorder.DETECTOR_INACTIVE.
   */
  protected SpeechDetectionMode _speechDetectionMode;

  /**
   * START_BEEP Boolean indicating whether subsequent record will be preceded
   * with a beep. Default: Boolean.TRUE
   */
  protected boolean _startBeep;

  /**
   * START_IN_PAUSED_MODE Boolean indicating whether subsequent record will
   * start in PAUSE mode. Default: Boolean.FALSE
   */
  protected boolean _startInPausedMode;

  /**
   * VIDEO_CODEC Parameter indicating the video codec to use for a new recording
   * (if it includes a video track). Default: INFERRED. For a list of Codec type
   * Values.
   */
  protected Value _videoCODEC;

  /**
   *VIDEO_FMTP A string-valued list of detailed audio codec parameters, in the
   * format described by rfc4566. for example: "bitrate=5.3;annexa=no". Default
   * value: empty string.
   */
  protected String _videoFMTP;

  /**
   * VIDEO_MAX_BITRATE The maximum accepted bitrate for the video stream. The
   * value is an Integer, in bits-per-second units.
   */
  protected int videoMaxBitRate;

  /**
   * BARGE_IN_ENABLED Controls whether the caller can start speaking before
   * prompts have ended.
   */
  protected boolean _promptBargeIn;
  
  public boolean isPromptBargeIn() {
    return _promptBargeIn;
  }

  public void setPromptBargeIn(boolean promptBargeIn) {
    this._promptBargeIn = promptBargeIn;
  }

  public RecordCommand(URL recordurl) {
    super();
    _recordURL = recordurl;
  }

  public URL getRecordURL() {
    return _recordURL;
  }

  public boolean isAppend() {
    return _append;
  }

  public void setAppend(boolean append) {
    this._append = append;
  }

  public int getAudioClockRate() {
    return _audioClockRate;
  }

  public void setAudioClockRate(int audioClockRate) {
    this._audioClockRate = audioClockRate;
  }

  public Value getAudioCODEC() {
    return _audioCODEC;
  }

  public void setAudioCODEC(Value audioCODEC) {
    this._audioCODEC = audioCODEC;
  }

  public String getAudioFMTP() {
    return _audioFMTP;
  }

  public void setAudioFMTP(String audioFMTP) {
    this._audioFMTP = audioFMTP;
  }

  public int getAudioMaxBitRate() {
    return _audioMaxBitRate;
  }

  public void setAudioMaxBitRate(int audioMaxBitRate) {
    this._audioMaxBitRate = audioMaxBitRate;
  }

  public int getBeepFrequency() {
    return _beepFrequency;
  }

  public void setBeepFrequency(int beepFrequency) {
    this._beepFrequency = beepFrequency;
  }

  public int getBeepLength() {
    return _beepLength;
  }

  public void setBeepLength(int beepLength) {
    this._beepLength = beepLength;
  }

  public Value getFileFormat() {
    return _fileFormat;
  }

  public void setFileFormat(Value fileFormat) {
    this._fileFormat = fileFormat;
  }

  public int getMaxDuration() {
    return _maxDuration;
  }

  public void setMaxDuration(int maxDuration) {
    this._maxDuration = maxDuration;
  }

  public int getMinDuration() {
    return _minDuration;
  }

  public void setMinDuration(int minDuration) {
    this._minDuration = minDuration;
  }

  public URI[] getPrompt() {
    return _prompt;
  }

  public void setPrompt(URI[] prompt) {
    this._prompt = prompt;
  }

  public boolean isSignalTruncationOn() {
    return _signalTruncationOn;
  }

  public void setSignalTruncationOn(boolean signalTruncationOn) {
    this._signalTruncationOn = signalTruncationOn;
  }

  public boolean isSilenceTerminationOn() {
    return _silenceTerminationOn;
  }

  public void setSilenceTerminationOn(boolean silenceTerminationOn) {
    this._silenceTerminationOn = silenceTerminationOn;
  }

  public SpeechDetectionMode getSpeechDetectionMode() {
    return _speechDetectionMode;
  }

  public void setSpeechDetectionMode(SpeechDetectionMode speechDetectionMode) {
    this._speechDetectionMode = speechDetectionMode;
  }

  public boolean isStartBeep() {
    return _startBeep;
  }

  public void setStartBeep(boolean startBeep) {
    this._startBeep = startBeep;
  }

  public boolean isStartInPausedMode() {
    return _startInPausedMode;
  }

  public void setStartInPausedMode(boolean startInPausedMode) {
    this._startInPausedMode = startInPausedMode;
  }

  public Value getVideoCODEC() {
    return _videoCODEC;
  }

  public void setVideoCODEC(Value videoCODEC) {
    this._videoCODEC = videoCODEC;
  }

  public String getVideoFMTP() {
    return _videoFMTP;
  }

  public void setVideoFMTP(String videoFMTP) {
    this._videoFMTP = videoFMTP;
  }

  public int getVideoMaxBitRate() {
    return videoMaxBitRate;
  }

  public void setVideoMaxBitRate(int videoMaxBitRate) {
    this.videoMaxBitRate = videoMaxBitRate;
  }

  public boolean isBargine() {
    return bargine;
  }

  public void setBargine(boolean bargine) {
    this.bargine = bargine;
  }
}

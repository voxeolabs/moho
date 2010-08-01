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

package com.voxeo.moho.media.output;

import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.Value;
import javax.media.mscontrol.mediagroup.CodecConstants;
import javax.media.mscontrol.mediagroup.FileFormatConstants;
import javax.media.mscontrol.resource.RTC;
import javax.media.mscontrol.resource.Resource;

public class OutputCommand {

  public enum BehaviorIfBusy {
    QUEUE, STOP, ERROR
  }

  protected AudibleResource[] _resources;

  protected BehaviorIfBusy _behavior = BehaviorIfBusy.QUEUE;

  protected boolean _bargein = false;

  /**
   * MAX_DURATION
   */
  protected int _timeout = Resource.FOREVER;

  /**
   * START_OFFSET
   */
  protected int _offset = 0;

  /**
   * VOLUME_CHANGE
   */
  protected int _volumeUnit = 3;

  /**
   * AUDIO_CODEC
   */
  protected Value _codec = CodecConstants.INFERRED;

  /**
   * FILE_FORMAT
   */
  protected Value _format = FileFormatConstants.INFERRED;

  /**
   * INTERVAL
   */
  protected int _repeatInterval = 3;

  /**
   * JUMP_PLAYLIST_INCREMENT Integer number of items to jump forward or backward
   * in the play list, for either a JUMP_FORWARD_IN_PLAYLIST or
   * JUMP_BACKWARD_IN_PLAYLIST. Default value = 1.
   */
  protected int _jumpPlaylistIncrement = 1;

  /**
   * JUMP_TIME Integer number of milliseconds by which the current play list
   * items offset is changed by JUMP_FORWARD or JUMP_BACKWARD.Default value =
   * 5000.
   */
  protected int _jumpTime = 5000;

  /**
   * START_IN_PAUSED_MODE Boolean indicating that this or subsequent play should
   * be started in the Paused state.
   */
  protected boolean _startInPausedMode = false;

  protected Parameters _parameters;

  protected RTC[] _rtcs;

  public OutputCommand(final AudibleResource resource) {
    if (resource != null) {
      _resources = new AudibleResource[] {resource};
    }
  }

  public OutputCommand(final AudibleResource[] resources) {
    _resources = resources;
  }

  public BehaviorIfBusy getBehavior() {
    return _behavior;
  }

  public void setBahavior(final BehaviorIfBusy behavior) {
    _behavior = behavior;
  }

  public AudibleResource[] getAudibleResources() {
    return _resources;
  }

  public void setAudibleResources(final AudibleResource[] resources) {
    _resources = resources;
  }

  public boolean isBargein() {
    return _bargein;
  }

  public void setBargein(final boolean bargein) {
    _bargein = bargein;
  }

  public int getTimeout() {
    return _timeout;
  }

  public void setTimeout(final int timeout) {
    _timeout = timeout;
  }

  public int getOffset() {
    return _offset;
  }

  public void setOffset(final int offset) {
    _offset = offset;
  }

  public int getVolumeUnit() {
    return _volumeUnit;
  }

  public void setVolumeUnit(final int unit) {
    _volumeUnit = unit;
  }

  public Value getCodec() {
    return _codec;
  }

  public void setCodec(final Value codec) {
    _codec = codec;
  }

  public Value getFormat() {
    return _format;
  }

  public void setFormat(final Value format) {
    _format = format;
  }

  public int getRepeatInterval() {
    return _repeatInterval;
  }

  public void setRepeatInterval(final int interval) {
    _repeatInterval = interval;
  }

  public int getJumpPlaylistIncrement() {
    return _jumpPlaylistIncrement;
  }

  public void setJumpPlaylistIncrement(int jumpPlaylistIncrement) {
    _jumpPlaylistIncrement = jumpPlaylistIncrement;
  }

  public int getJumpTime() {
    return _jumpTime;
  }

  public void setJumpTime(int jumpTime) {
    _jumpTime = jumpTime;
  }

  public boolean isStartInPausedMode() {
    return _startInPausedMode;
  }

  public void setStartInPausedMode(boolean startInPausedMode) {
    _startInPausedMode = startInPausedMode;
  }

  public Parameters getParameters() {
    return _parameters;
  }

  public void setParameters(Parameters parameters) {
    _parameters = parameters;
  }

  public RTC[] getRtcs() {
    return _rtcs;
  }

  public void setRtcs(RTC[] rtcs) {
    _rtcs = rtcs;
  }
}

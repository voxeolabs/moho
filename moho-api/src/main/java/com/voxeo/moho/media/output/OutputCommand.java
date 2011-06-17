/**
 * Copyright 2010-2011 Voxeo Corporation
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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.media.mscontrol.Parameter;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.Value;
import javax.media.mscontrol.mediagroup.CodecConstants;
import javax.media.mscontrol.mediagroup.FileFormatConstants;
import javax.media.mscontrol.resource.RTC;
import javax.media.mscontrol.resource.Resource;

import org.apache.log4j.Logger;

public class OutputCommand implements Parameters {

  private static final Logger log = Logger.getLogger(OutputCommand.class);

  public enum BehaviorIfBusy {
    QUEUE, STOP, ERROR
  }

  protected AudibleResource[] _resources;

  protected BehaviorIfBusy _behavior = BehaviorIfBusy.QUEUE;

  protected boolean _bargein = false;
  
  protected String _voiceName;

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
   * INTERVAL
   */
  protected int _repeatTimes = 0;

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

  protected Map<Parameter, Object> _parametersExt = new HashMap<Parameter, Object>();

  protected Set<RTC> _rtcsExt = new HashSet<RTC>();

  /**
   * @param textOrURL
   */
  public OutputCommand(String textOrURL) {
    if (textOrURL == null || textOrURL.length() == 0) {
      throw new IllegalArgumentException();
    }
    URL url = null;
    try {
      url = new URL(textOrURL);
    }
    catch (IllegalArgumentException ex) {
      // IGNORE
      log.debug("" + ex.getMessage());
    }
    catch (MalformedURLException e) {
      // IGNORE
      log.debug("" + e.getMessage());
    }
    if (url != null) {
      _resources = new AudibleResource[] {new AudioURIResource(URI.create(textOrURL))};
    }
    else {
      _resources = new AudibleResource[] {new TextToSpeechResource(textOrURL)};
    }
  }

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

  @Deprecated
  public Parameters getParameters() {
    return _parameters;
  }

  @Deprecated
  public void setParameters(Parameters parameters) {
    _parameters = parameters;
  }

  @Deprecated
  public RTC[] getRtcs() {
    return _rtcs;
  }

  @Deprecated
  public void setRtcs(RTC[] rtcs) {
    _rtcs = rtcs;
  }

  // method for RTC
  public boolean addRTC(RTC o) {
    return _rtcsExt.add(o);
  }

  public boolean addAllRTC(Collection<? extends RTC> c) {
    return _rtcsExt.addAll(c);
  }

  public void removeAllRTC() {
    _rtcsExt.clear();
  }

  public boolean removeRTC(Object o) {
    return _rtcsExt.remove(o);
  }

  public Set<RTC> getAllRTC() {
    return _rtcsExt;
  }

  // method for Parameters
  @Override
  public void clear() {
    _parametersExt.clear();
  }

  @Override
  public boolean containsKey(Object key) {
    return _parametersExt.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return _parametersExt.containsValue(value);
  }

  @Override
  public Set<java.util.Map.Entry<Parameter, Object>> entrySet() {
    return _parametersExt.entrySet();
  }

  @Override
  public Object get(Object key) {
    return _parametersExt.get(key);
  }

  @Override
  public boolean isEmpty() {
    return _parametersExt.isEmpty();
  }

  @Override
  public Set<Parameter> keySet() {
    return _parametersExt.keySet();
  }

  @Override
  public Object put(Parameter key, Object value) {
    return _parametersExt.put(key, value);
  }

  @Override
  public void putAll(Map<? extends Parameter, ? extends Object> t) {
    _parametersExt.putAll(t);
  }

  @Override
  public Object remove(Object key) {
    return _parametersExt.remove(key);
  }

  @Override
  public int size() {
    return _parametersExt.size();
  }

  @Override
  public Collection<Object> values() {
    return _parametersExt.values();
  }

  public int getRepeatTimes() {
    return _repeatTimes;
  }

  public void setRepeatTimes(int repeatTimes) {
    _repeatTimes = repeatTimes;
  }
  
  public String getVoiceName() {
      return _voiceName;
  }

  public void setVoiceName(String voiceName) {
      _voiceName = voiceName;
  }
  
}

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

/**
 * <p>Media command to play an array of {@link AudibleResource AudibleResource}.</p>
 * 
 * <p>When an array of {@link AudibleSpeechResource AudibleResource}
 * are given to this command, these {@link AudibleSpeechResource AudibleResource} form
 * a play list. You can jump forwards or backwards within the list.</p>
 * 
 * <p> When applicable, the properties of this command apply to all the 
 * {@link AudibleSpeechResource AudibleResource}
 * in the array. If different properties are required for different 
 * {@link AudibleSpeechResource AudibleResource}, 
 * you have to use separate {@link OutputCommand OutputCommand} for each 
 * {@link AudibleSpeechResource AudibleResource}.</p>
 * 
 * @author wchen
 *
 * TODO: 
 *   move forward/backward on TTS resource.
 */
public class OutputCommand implements Parameters {

  private static final Logger LOG = Logger.getLogger(OutputCommand.class);

  /**
   * How media server should behave when it receives an {@link OutputCommand OutputCommand} 
   * while it is playing other {@link AudibleResource AudibleResource}.
   *
   */
  public enum BehaviorIfBusy {
    /**
     * media server should queue up this command and play later.
     */
    QUEUE, 
    /**
     * media server should stop playing what it is playing
     * but play this command immediately.
     */
    STOP, 
    /**
     * media server should continue playing what it is  playing 
     * but return an error for this command.
     */
    ERROR
  }

  protected AudibleResource[] _resources;

  protected BehaviorIfBusy _behavior = BehaviorIfBusy.QUEUE;

  protected boolean _bargein = false;
  
  protected String _voiceName;

  protected int _maxtime = Resource.FOREVER;

  protected int _offset = 0;

  protected int _volumeUnit = 3;

  protected Value _codec = CodecConstants.INFERRED;

  protected Value _format = FileFormatConstants.INFERRED;

  protected int _repeatInterval = 0;

  protected int _repeatTimes = 0;

  protected int _jumpPlaylistIncrement = 1;

  protected int _jumpTime = 5000;

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
      LOG.debug("" + ex.getMessage());
    }
    catch (MalformedURLException e) {
      // IGNORE
      LOG.debug("" + e.getMessage());
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

  /**
   * @return What this command should do when the server is busy. 
   * The default value is {@link OutputCommand.BehaviorIfBusy.QUEUE QUEUE}.
   */
  public BehaviorIfBusy getBehavior() {
    return _behavior;
  }

  /**
   * Set What this command should do when the server is busy.
   * @param behavior
   */
  public void setBahavior(final BehaviorIfBusy behavior) {
    _behavior = behavior;
  }
  
  /**
   * @return The array of {@link AudibleResource AudibleResource} to be played within this command.
   * This array of {@link AudibleResource AudibleResource} sometimes is referred as play list.
   */
  public AudibleResource[] getAudibleResources() {
    return _resources;
  }

//  public void setAudibleResources(final AudibleResource[] resources) {
//    _resources = resources;
//  }

  /**
   * @return Whether this command should be stopped when the other end speaks
   */
  public boolean isBargein() {
    return _bargein;
  }

  /**
   * @param bargein true if this command should be stopped when the other end speaks
   */
  public void setBargein(final boolean bargein) {
    _bargein = bargein;
  }

  /**
   * The max time of each {@link AudibleResource AudibleResource} in this command can be played.
   * When the time is reached, the current {@link AudibleResource AudibleResource} will be stopped
   * and next {@link AudibleResource AudibleResource} will start to play.
   * 
   * @return the max timeout in milliseconds.
   */
  public long getMaxtime() {
    return _maxtime;
  }

  /**
   * set the max timeout of each {@link AudibleResource AudibleResource} in this command.
   * @param maxtime the max timeout in milliseconds.
   */
  public void setMaxtime(final int maxtime) {
    if (maxtime <= 0) {
      throw new IllegalArgumentException("Timeout must be a positive integer.");
    }
    _maxtime = maxtime;
  }

  /**
   * @return The starting offset, in milliseconds, of each {@link AudibleResource AudibleResource} in this command.
   */
  public int getOffset() {
    return _offset;
  }

  /**
   * Set the starting offset of each {@link AudibleResource AudibleResource} in this command.
   * @param offset the starting offset in millisecond.
   */
  public void setOffset(final int offset) {
    if (offset < 0) {
      throw new IllegalArgumentException("Offset can not be a negative integer.");
    }
    _offset = offset;
  }

  /**
   * @return The unit of the volume, in dB, to be increased or decreased 
   * when the {@link com.voxeo.moho.media.Output#volume(boolean) Output.volume()} is invoked.
   * The default value is 3db.
   * 
   * @see com.voxeo.moho.media.Output#volume(boolean)
   */
  public int getVolumeUnit() {
    return _volumeUnit;
  }

  /**
   * Set the unit of volume to increased or decreased when the 
   * {@link com.voxeo.moho.media.Output.volume(boolean) Output.volume(boolean)} is invoked.
   * 
   * @param unit The unit of volume in dB.
   */
  public void setVolumeUnit(final int unit) {
    _volumeUnit = unit;
  }

  /**
   * @return The default codec value of the each {@link AudibleResource AudibleResource}, 
   * if applicable, in this command.
   * By default, the codec is inferred from the file extension and format.
   */
  public Value getCodec() {
    return _codec;
  }

  /**
   * Set the default codec value for this command.
   * @param codec One of the <a href="http://micromethod.com/api/msctrl-1.0/javax/media/mscontrol/mediagroup/CodecConstants.html">codec constants</a>.
   */
  public void setCodec(final Value codec) {
    _codec = codec;
  }

  /**
   * @return The file format of the each {@link AudibleResource AudibleResource}, 
   * if applicable, in this command.
   * By default, the format is inferred from the file extension.
   */
  public Value getFormat() {
    return _format;
  }

  /**
   * Set the default file format for this command.
   * @param format One of the <a href="http://micromethod.com/api/msctrl-1.0/javax/media/mscontrol/mediagroup/FileFormatConstants.html">file format constants</a>.
   */
  public void setFormat(final Value format) {
    _format = format;
  }

  /**
   * @return How long, in milliseconds, the wait time is between each repetition of this command.
   * Only applicable when {link {@link #getRepeatTimes() getRepeatTimes()} is greater than 1.
   * The default value is 0.
   */
  public int getRepeatInterval() {
    return _repeatInterval;
  }

  /**
   * set the interval between each repetition of this command.
   * @param interval in milliseconds.
   */
  public void setRepeatInterval(final int interval) {
    if (interval < 0) {
      throw new IllegalArgumentException("Repeat Interval can not be a negative integer.");
    }
    _repeatInterval = interval;
  }

  /**
   * @return The unit of jump size when {@link com.voxeo.moho.media.Output#jump(int) Output.jump(number)} is invoked.
   * The default value is 1.
   * 
   * @see com.voxeo.moho.media.Output#jump(int)
   */
  public int getJumpPlaylistIncrement() {
    return _jumpPlaylistIncrement;
  }

  /**
   * Set the unit of jump size. 
   * @param jumpPlaylistIncrement unit as a positive integer.
   */
  public void setJumpPlaylistIncrement(int jumpPlaylistIncrement) {
    if (jumpPlaylistIncrement <=0) {
      throw new IllegalArgumentException("Jump Playlist Increment must be a positive integer.");
    }
    _jumpPlaylistIncrement = jumpPlaylistIncrement;
  }

  /**
   * @return The unit of the move time, in milliseconds, 
   * when {@link com.voxeo.moho.media.Output#move(boolean, long) Output.move(direction, time)} is invoked.
   * 
   * @see com.voxeo.moho.media.Output#move(boolean, long)
   */
  public int getMoveTime() {
    return _jumpTime;
  }

  /**
   * Set the unit of move time in milliseconds.
   * @param jumpTime
   */
  public void setMoveTime(int jumpTime) {
    if (jumpTime < 0) {
      throw new IllegalArgumentException("Time can not be a negative integer.");
    }
    _jumpTime = jumpTime;
  }

  /**
   * @return Whether this command should be started in pause 
   * so it can be com.voxeo.moho.media.Output#resume() resumed) later.
   */
  public boolean isStartInPausedMode() {
    return _startInPausedMode;
  }

  /**
   * Set whether this command should be started in pause mode.
   * @param startInPausedMode true if this command should be started in pause mode.
   */
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

  /**
   * Add <a href="http://micromethod.com/api/msctrl-1.0/javax/media/mscontrol/resource/RTC.html">RTC</a> to this command.
   * @param rtc the <a href="http://micromethod.com/api/msctrl-1.0/javax/media/mscontrol/resource/RTC.html">RTC</a> to be added.
   * @return true if rtc is added.
   */
  public boolean addRTC(RTC rtc) {
    return _rtcsExt.add(rtc);
  }

  /**
   * Add a collection of <a href="http://micromethod.com/api/msctrl-1.0/javax/media/mscontrol/resource/RTC.html">RTC</a> to this command.
   * @param c the collection of <a href="http://micromethod.com/api/msctrl-1.0/javax/media/mscontrol/resource/RTC.html">RTC</a>.
   * @return true if the collection is successfully added.
   */
  public boolean addAllRTC(Collection<? extends RTC> c) {
    return _rtcsExt.addAll(c);
  }

  /**
   * Remove all the <a href="http://micromethod.com/api/msctrl-1.0/javax/media/mscontrol/resource/RTC.html">RTC</a>s.
   */
  public void removeAllRTC() {
    _rtcsExt.clear();
  }

  /**
   * Remove a <a href="http://micromethod.com/api/msctrl-1.0/javax/media/mscontrol/resource/RTC.html">RTC</a>.
   * @param o the <a href="http://micromethod.com/api/msctrl-1.0/javax/media/mscontrol/resource/RTC.html">RTC</a> to be removed.
   * @return true if removed.
   */
  public boolean removeRTC(Object o) {
    return _rtcsExt.remove(o);
  }

  /**
   * @return all the <a href="http://micromethod.com/api/msctrl-1.0/javax/media/mscontrol/resource/RTC.html">RTC</a> within this command.
   */
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

  /**
   * @return How many times this command should be repeated.
   */
  public int getRepeatTimes() {
    return _repeatTimes;
  }

  /**
   * Set how many times this command should be repeated.
   * @param repeatTimes
   */
  public void setRepeatTimes(int repeatTimes) {
    _repeatTimes = repeatTimes;
  }
  
  /**
   * @return The voice name for each {@link TextToSpeechResource TextToSpeechResource} in this command.
   */
  public String getVoiceName() {
      return _voiceName;
  }

  /**
   * Set the voice name for this command.
   * The interpretation of voice name is 
   * {@link com.voxeo.moho.media.dialect.MediaDialect MediaDialect} specific.
   * @param voiceName the voice name to be set.
   */
  public void setVoiceName(String voiceName) {
      _voiceName = voiceName;
  }
}

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

package com.voxeo.moho.media.input;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.media.mscontrol.Parameter;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.resource.RTC;
import javax.media.mscontrol.resource.Resource;

public class InputCommand implements Parameters {

  protected int _signalNumber = -1;

  protected Grammar[] _grammars = new Grammar[0];

  protected float _sensitive = 0.5f;

  protected long _initialTimeout = Resource.FOREVER;

  protected long _interSigTimeout = Resource.FOREVER;

  protected long _maxTimeout = Resource.FOREVER;

  protected boolean _record = false;

  protected URI _recordURI = null;

  protected boolean _buffering = true;

  protected Parameters _parameters;

  protected RTC[] _rtcs;

  protected Map<Parameter, Object> _parametersExt = new HashMap<Parameter, Object>();

  protected Set<RTC> _rtcsExt = new HashSet<RTC>();

  /**
   * if true, every DTMF (or word?) received generate a
   */
  protected boolean _supervised = true;

  /**
   * @param grammers
   *          can be simple string or string that starts with "#JSGF". if the
   *          string starts with "#JSGF", a JSGF grammar will be created.
   */
  public InputCommand(String grammer) {
    if (grammer == null || grammer.length() == 0) {
      throw new IllegalArgumentException();
    }
    _grammars = new Grammar[] {Grammar.create(grammer)};
  }

  public InputCommand(final Grammar... grammars) {
    if (grammars != null && grammars.length > 0) {
      _grammars = grammars;
    }
  }

  public long getInterSigTimeout() {
    return _interSigTimeout;
  }

  public void setInterSigTimeout(final long time) {
    _interSigTimeout = time;
  }

  public boolean isBuffering() {
    return _buffering;
  }

  public void setBuffering(final boolean buffering) {
    _buffering = buffering;
  }

  public int getSignalNumber() {
    return _signalNumber;
  }

  public void setSignalNumber(final int num) {
    _signalNumber = num;
  }

  public Grammar[] getGrammars() {
    return _grammars;
  }

  public float getConfidence() {
    return _sensitive;
  }

  public boolean isRecord() {
    return _record;
  }

  public void setRecord(final boolean flag) {
    _record = flag;
  }

  public URI getRecordURI() {
    return _recordURI;
  }

  public void setRecordURI(final URI uri) {
    _recordURI = uri;
  }

  public void setSensitive(final float sensitive) {
    _sensitive = sensitive;
  }

  public long getInitialTimeout() {
    return _initialTimeout;
  }

  public void setInitialTimeout(final long time) {
    _initialTimeout = time;
  }

  public long getMaxTimeout() {
    return _maxTimeout;
  }

  public void setMaxTimeout(final long time) {
    _maxTimeout = time;
  }

  public void setSupervised(final boolean supervised) {
    _supervised = supervised;
  }

  public boolean isSupervised() {
    return _supervised;
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
}

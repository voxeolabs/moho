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

import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.resource.RTC;
import javax.media.mscontrol.resource.Resource;

public class InputCommand {

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

  /**
   * if true, every DTMF (or word?) received generate a
   */
  protected boolean _supervised = true;

  public InputCommand() {

  }

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

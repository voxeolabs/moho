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

import java.net.URL;

import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.resource.RTC;
import javax.media.mscontrol.resource.Resource;

public class InputCommand {

  public enum Type {
    DTMF, SPEECH, ANY
  }

  protected int _signalNumber = -1;

  protected Grammar[] _grammars = new Grammar[0];

  protected float _confidence = 0.5f;

  protected Type _type = Type.ANY;

  protected long _initialTimeout = Resource.FOREVER;

  protected long _interSigTimeout = Resource.FOREVER;

  protected long _maxTimeout = Resource.FOREVER;

  protected boolean _record = false;

  protected URL _recordURL = null;

  protected boolean _buffering = true;

  protected Parameters _parameters;

  protected RTC[] _rtcs;

  protected String _terminateChar;

  /**
   * if true, every DTMF (or word?) received generate a
   */
  protected boolean _supervised = true;

  public InputCommand(final Grammar... grammars) {
    if (grammars != null && grammars.length > 0) {
      _grammars = grammars;
    }
  }

  public String getTerminateChar() {
    return _terminateChar;
  }

  public void setTerminateChar(char terminateChar) {
    this._terminateChar = String.valueOf(terminateChar);
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
    return _confidence;
  }

  public boolean isRecord() {
    return _record;
  }

  public void setRecord(final boolean flag) {
    _record = flag;
  }

  public URL getRecordURL() {
    return _recordURL;
  }

  public void setRecordURL(final URL url) {
    _recordURL = url;
  }

  public void setConfidence(final float confidence) {
    _confidence = confidence;
  }

  public Type getType() {
    return _type;
  }

  public void setType(final Type type) {
    _type = type;
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

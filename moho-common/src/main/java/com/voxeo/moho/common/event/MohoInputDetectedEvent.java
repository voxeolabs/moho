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

package com.voxeo.moho.common.event;

import java.util.Map;

import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.InputDetectedEvent;
import com.voxeo.moho.media.InputMode;
import com.voxeo.moho.media.input.SignalGrammar.Signal;

public class MohoInputDetectedEvent<T extends EventSource> extends MohoMediaNotificationEvent<T> implements
    InputDetectedEvent<T> {

  protected String _input = null;

  protected String _concept;

  protected String _interpretation;

  protected float _confidence;

  protected String _nlsml;

  protected String _tag;

  protected InputMode _inputMode;

  protected Map<String, String> _SISlots;

  protected boolean _isEOS;

  protected boolean _isSOS;

  protected Signal _signal;

  public MohoInputDetectedEvent(final T source, final String input) {
    super(source);
    _input = input;
  }

  public MohoInputDetectedEvent(final T source, final Signal signal) {
    super(source);
    this._signal = signal;
  }

  public MohoInputDetectedEvent(final T source, final boolean startOfSpeech, final boolean endOfSpeech) {
    super(source);
    this._isEOS = endOfSpeech;
    this._isSOS = startOfSpeech;
  }

  @Override
  public String getInput() {
    return _input;
  }

  @Override
  public String getConcept() {
    return _concept;
  }

  public void setConcept(final String concept) {
    this._concept = concept;
  }

  @Override
  public String getInterpretation() {
    return _interpretation;
  }

  public void setInterpretation(final String interpretation) {
    this._interpretation = interpretation;
  }

  @Override
  public float getConfidence() {
    return _confidence;
  }

  public void setConfidence(final float confidence) {
    this._confidence = confidence;
  }

  @Override
  public String getNlsml() {
    return _nlsml;
  }

  public void setNlsml(final String _nlsml) {
    this._nlsml = _nlsml;
  }

  @Override
  public String getTag() {
    return _tag;
  }

  public void setTag(final String tag) {
    _tag = tag;
  }

  @Override
  public InputMode getInputMode() {
    return _inputMode;
  }

  public void setInputMode(InputMode inputMode) {
    _inputMode = inputMode;
  }

  @Override
  public Map<String, String> getSISlots() {
    return _SISlots;
  }

  public void setSISlots(Map<String, String> slots) {
    _SISlots = slots;
  }

  @Override
  public boolean isStartOfSpeech() {
    return _isSOS;
  }

  @Override
  public boolean isEndOfSpeech() {
    return _isEOS;
  }

  @Override
  public Signal getSignal() {
    return _signal;
  }

  @Override
  public String toString() {
    return String.format("[Event class=%s source=%s id=%s input=%s eos=%s sos=%s signal=%s]",
        getClass().getSimpleName(), source, hashCode(), _input, _isEOS,
        _isSOS, _signal);
  }
}

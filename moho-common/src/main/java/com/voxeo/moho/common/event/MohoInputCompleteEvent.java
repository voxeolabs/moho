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
import com.voxeo.moho.event.InputCompleteEvent;
import com.voxeo.moho.media.Input;
import com.voxeo.moho.media.InputMode;
import com.voxeo.moho.media.input.SignalGrammar.Signal;

public class MohoInputCompleteEvent<T extends EventSource> extends MohoMediaCompleteEvent<T> implements
    InputCompleteEvent<T> {

  protected Cause _cause;

  protected String _concept;

  protected String _interpretation;

  protected String _utterance;

  protected float _confidence;

  protected String _nlsml;

  protected String _tag;

  protected boolean _successful;

  protected InputMode _inputMode;

  protected String _errorText;

  protected Map<String, String> _SISlots;
  
  protected Signal _signal;

  public MohoInputCompleteEvent(final T source, final Cause cause, Input<T> mediaOperation) {
    super(source, mediaOperation);
    _cause = cause;
    if (_cause == Cause.MATCH) {
      _successful = true;
    }
  }

  public MohoInputCompleteEvent(final T source, final Cause cause, String errorText, Input<T> mediaOperation) {
    this(source, cause, mediaOperation);
    _errorText = errorText;
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
  public String getUtterance() {
    return _utterance;
  }

  public void setUtterance(final String utterance) {
    this._utterance = utterance;
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
  public Cause getCause() {
    return _cause;
  }

  @Override
  public boolean hasMatch() {
    return _successful;
  }

  @Override
  public InputMode getInputMode() {
    return _inputMode;
  }

  public void setInputMode(InputMode inputMode) {
    _inputMode = inputMode;
  }

  @Override
  public String getValue() {
    String retval = getConcept();
    if (retval == null) {
      retval = getUtterance();
    }
    if (retval == null) {
      retval = getInterpretation();
    }
    return retval;
  }

  @Override
  public String getErrorText() {
    return _errorText;
  }

  @Override
  public Map<String, String> getSISlots() {
    return _SISlots;
  }

  public void setSISlots(Map<String, String> slots) {
    _SISlots = slots;
  }
  
  @Override
  public Signal getSignal() {
    return _signal;
  }

  public void setSignal(final Signal signal) {
    _signal = signal;
  }

  @Override
  public String toString() {
    return String.format("[Event class=%s sourceClass=%s id=%s cause=%s ]", getClass().getName(),
        (source != null ? source.getClass().getSimpleName() : null), hashCode(), _cause);
  }
}

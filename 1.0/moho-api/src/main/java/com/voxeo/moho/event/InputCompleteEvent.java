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

package com.voxeo.moho.event;

public class InputCompleteEvent extends MediaCompleteEvent {

  public enum Cause {
    /** the input is terminated because the initial silence is too long */
    INI_TIMEOUT,
    /** the input is terminated because the INTER_SIG_TIMEOUT_EXCEEDED */
    IS_TIMEOUT,
    /** the input is terminated by exceeding its max time allowed */
    MAX_TIMEOUT,
    /** the input is terminated by unknown error */
    ERROR,
    /** the input is canceled */
    CANCEL,
    /** the input is completed without a match */
    NO_MATCH,
    /** the input is completed with a match */
    MATCH,
    /** the input is terminated because the source is disconnected */
    DISCONNECT, UNKNOWN
  }

  private static final long serialVersionUID = 4354478901698920065L;

  protected Cause _cause;

  protected String _concept;

  protected String _interpretation;

  protected String _utterance;

  protected float _confidence;

  protected String _nlsml;

  protected boolean successful;

  public InputCompleteEvent(final EventSource source, final Cause cause) {
    super(source);
    _cause = cause;
    if (_cause == Cause.MATCH) {
      successful = true;
    }
  }

  public String getConcept() {
    return _concept;
  }

  public void setConcept(final String _concept) {
    this._concept = _concept;
  }

  public String getInterpretation() {
    return _interpretation;
  }

  public void setInterpretation(final String _interpretation) {
    this._interpretation = _interpretation;
  }

  public String getUtterance() {
    return _utterance;
  }

  public void setUtterance(final String _utterance) {
    this._utterance = _utterance;
  }

  public float getConfidence() {
    return _confidence;
  }

  public void setConfidence(final float _confidence) {
    this._confidence = _confidence;
  }

  public String get_nlsml() {
    return _nlsml;
  }

  public void set_nlsml(final String _nlsml) {
    this._nlsml = _nlsml;
  }

  public Cause getCause() {
    return _cause;
  }

  public boolean hasMatch() {
    return successful;
  }

}

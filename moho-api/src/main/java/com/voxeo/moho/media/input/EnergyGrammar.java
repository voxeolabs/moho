package com.voxeo.moho.media.input;

public class EnergyGrammar extends Grammar {

  protected final boolean _startOfSpeech;

  protected final boolean _endOfSpeech;

  public EnergyGrammar(boolean startOfSpeech, boolean endOfSpeech, boolean terminating) {
    super(null, null, terminating);
    this._startOfSpeech = startOfSpeech;
    this._endOfSpeech = endOfSpeech;
  }

  public boolean isStartOfSpeech() {
    return _startOfSpeech;
  }

  public boolean isEndOfSpeech() {
    return _endOfSpeech;
  }

}
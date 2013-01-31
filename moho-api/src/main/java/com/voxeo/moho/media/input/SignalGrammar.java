package com.voxeo.moho.media.input;

public class SignalGrammar extends Grammar {

  public enum Signal {
    FAX_CED, FAX_CNG, BEEP, RING, SIT, MODEM, OFFHOOK
  }

  protected final Signal _signal;

  public SignalGrammar(final Signal signal) {
    _signal = signal;
  }

  public Signal getSignal() {
    return _signal;
  }
}

package com.voxeo.moho.media;

public class InputPattern {

  private final int index;

  private final Object value;

  private final boolean isTerminating;

  public InputPattern(int index, Object value, boolean isTerminating) {
    super();
    this.index = index;
    this.value = value;
    this.isTerminating = isTerminating;
  }

  public int getIndex() {
    return index;
  }

  public Object getValue() {
    return value;
  }

  public boolean isTerminating() {
    return isTerminating;
  }

}

package com.voxeo.moho.media;

public class InputPattern {

  /** JSR-309 pattern index **/
  private final int index;

  /** JSR-309 pattern value **/
  private final Object value;

  /**
   * Specify whether to terminate the detection operation once the user input
   * matched with this pattern, otherwise the detection will be kept running
   * until other terminationg conditions (such as timeout, manually stop, etc)
   * occur.
   **/
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

  @Override
  public int hashCode() {
    return index;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj != null && obj instanceof InputPattern) {
      final InputPattern anotherpattern = (InputPattern) obj;
      return this.index == anotherpattern.index && this.value.equals(anotherpattern.value)
          && this.isTerminating == anotherpattern.isTerminating;
    }
    return false;
  }

  @Override
  public String toString() {
    return String.format("[InputPattern index=%s value=%s isTerminating=%s]", index, value, isTerminating);
  }

}

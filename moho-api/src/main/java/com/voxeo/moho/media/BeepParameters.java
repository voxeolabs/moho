package com.voxeo.moho.media;

public class BeepParameters {

  protected long onTime = 120;

  protected long offTime = 120;

  protected long minFrequency = 300;

  protected long maxFrequency = 3000;

  protected float frequencyTolerance = 1.1f;

  protected float monotonicity = 0.8f;

  protected float signalToNoise = 0.8f;

  protected float highMonotonicity = 1f;

  protected int minPower = -30;

  protected long maxNoiseTime = 256;

  protected long maxConsecutiveNoiseTime = 128;

  public BeepParameters() {
    super();
  }

  public void setOnTime(long onTime) {
    this.onTime = onTime;
  }

  public void setOffTime(long offTime) {
    this.offTime = offTime;
  }

  public void setMinFrequency(long minFrequency) {
    this.minFrequency = minFrequency;
  }

  public void setMaxFrequency(long maxFrequency) {
    this.maxFrequency = maxFrequency;
  }

  public void setFrequencyTolerance(float frequencyTolerance) {
    this.frequencyTolerance = frequencyTolerance;
  }

  public void setMonotonicity(float monotonicity) {
    this.monotonicity = monotonicity;
  }

  public void setSignalToNoise(float signalToNoise) {
    this.signalToNoise = signalToNoise;
  }

  public void setHighMonotonicity(float highMonotonicity) {
    this.highMonotonicity = highMonotonicity;
  }

  public void setMinPower(int minPower) {
    this.minPower = minPower;
  }

  public void setMaxNoiseTime(long maxNoiseTime) {
    this.maxNoiseTime = maxNoiseTime;
  }

  public void setMaxConsecutiveNoiseTime(long maxConsecutiveNoiseTime) {
    this.maxConsecutiveNoiseTime = maxConsecutiveNoiseTime;
  }

  public long getOnTime() {
    return onTime;
  }

  public long getOffTime() {
    return offTime;
  }

  public long getMinFrequency() {
    return minFrequency;
  }

  public long getMaxFrequency() {
    return maxFrequency;
  }

  public float getFrequencyTolerance() {
    return frequencyTolerance;
  }

  public float getMonotonicity() {
    return monotonicity;
  }

  public float getSignalToNoise() {
    return signalToNoise;
  }

  public float getHighMonotonicity() {
    return highMonotonicity;
  }

  public int getMinPower() {
    return minPower;
  }

  public long getMaxNoiseTime() {
    return maxNoiseTime;
  }

  public long getMaxConsecutiveNoiseTime() {
    return maxConsecutiveNoiseTime;
  }

}

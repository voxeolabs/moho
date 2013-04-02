package com.voxeo.moho.media;

public class EnergyParameters {

  protected long finalSilence = 1000;

  protected long maxSilence = -1;

  protected long maxSpeechDuration = -1;

  protected long minSpeechDuration = 80;

  protected int minVolume = -24;

  public EnergyParameters() {
    super();
    // TODO Auto-generated constructor stub
  }

  public void setFinalSilence(long finalSilence) {
    this.finalSilence = finalSilence;
  }

  public void setMaxSilence(long maxSilence) {
    this.maxSilence = maxSilence;
  }

  public void setMaxSpeechDuration(long maxSpeechDuration) {
    this.maxSpeechDuration = maxSpeechDuration;
  }

  public void setMinSpeechDuration(long minSpeechDuration) {
    this.minSpeechDuration = minSpeechDuration;
  }

  public void setMinVolume(int minVolume) {
    this.minVolume = minVolume;
  }

  public long getFinalSilence() {
    return finalSilence;
  }

  public long getMaxSilence() {
    return maxSilence;
  }

  public long getMaxSpeechDuration() {
    return maxSpeechDuration;
  }

  public long getMinSpeechDuration() {
    return minSpeechDuration;
  }

  public int getMinVolume() {
    return minVolume;
  }

}

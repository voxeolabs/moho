package com.voxeo.moho.media;

import javax.media.mscontrol.EventType;
import javax.media.mscontrol.MediaErr;
import javax.media.mscontrol.Qualifier;
import javax.media.mscontrol.networkconnection.SdpPortManager;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.media.mscontrol.resource.Trigger;

public class FakeSdpPortManagerEvent implements SdpPortManagerEvent {

  private SdpPortManagerEvent _realSdpPortManagerEvent;

  public FakeSdpPortManagerEvent(SdpPortManagerEvent realSdpPortManagerEvent) {
    super();
    this._realSdpPortManagerEvent = realSdpPortManagerEvent;
  }

  @Override
  public Qualifier getQualifier() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Trigger getRTCTrigger() {
    return _realSdpPortManagerEvent.getRTCTrigger();
  }

  @Override
  public MediaErr getError() {
    return _realSdpPortManagerEvent.getError();
  }

  @Override
  public String getErrorText() {
    return _realSdpPortManagerEvent.getErrorText();
  }

  @Override
  public EventType getEventType() {
    return _realSdpPortManagerEvent.getEventType();
  }

  @Override
  public SdpPortManager getSource() {
    return _realSdpPortManagerEvent.getSource();
  }

  @Override
  public boolean isSuccessful() {
    return _realSdpPortManagerEvent.isSuccessful();
  }

  @Override
  public byte[] getMediaServerSdp() {
    // TODO Auto-generated method stub
    return null;
  }

}

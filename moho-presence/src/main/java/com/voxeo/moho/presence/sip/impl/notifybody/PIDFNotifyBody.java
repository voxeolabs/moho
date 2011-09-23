package com.voxeo.moho.presence.sip.impl.notifybody;


public class PIDFNotifyBody extends SimpleNotifyBody {
  
  public PIDFNotifyBody(String content) {
    super(content);
  }
  
  public PIDFNotifyBody(String encoding, byte[] content) {
    super(encoding, content);
  }

  @Override
  public String getName() {
    return "application/pidf+xml";
  }
}

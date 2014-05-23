package com.voxeo.moho.util;

import java.io.IOException;

import javax.sdp.Origin;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;

import org.apache.log4j.Logger;

import com.voxeo.moho.sip.SIPCallImpl;
import com.voxeo.moho.spi.ExecutionContext;

public class SDPUtils {
  private static final Logger LOG = Logger.getLogger(SDPUtils.class);

  private static SdpFactory sdpFactory;

  private static String SDPUtils_PreviousOrigin_Attribute = "SDPUtils_PreviousOrigin_Attribute";

  public static void init(ExecutionContext context) {
    sdpFactory = context.getSdpFactory();
  }

  public static byte[] formulateSDP(SIPCallImpl call, Object sdp) throws IOException {

    SessionDescription sd = null;
    if (sdp instanceof byte[]) {
      try {
        sd = sdpFactory.createSessionDescription(new String((byte[]) sdp, "iso8859-1"));
      }
      catch (Exception e) {
        LOG.warn("Exception when parsing SDP.", e);
        return (byte[]) sdp;
      }
    }
    else {
      sd = (SessionDescription) sdp;
    }

    try {
      Origin previousOrigin = call.getPreviousOrigin();
      if (previousOrigin != null) {
        previousOrigin.setSessionVersion(previousOrigin.getSessionVersion() + 1);
        sd.setOrigin(previousOrigin);
      }
      else {
        call.setPreviousOrigin(sd.getOrigin());
      }
    }
    catch (Exception ex) {
      LOG.warn("Exception when parsing SDP.", ex);
    }

    byte[] newSDP = sd.toString().getBytes("iso8859-1");
    call.setLocalSDP(newSDP);
    return newSDP;
  }
}

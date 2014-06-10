package com.voxeo.moho.util;

import java.io.IOException;

import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;

import com.voxeo.moho.spi.ExecutionContext;

public class SDPUtils {

  private static SdpFactory sdpFactory;

  public static void init(ExecutionContext context) {
    sdpFactory = context.getSdpFactory();
  }

  public static byte[] makeBlackholeSDP(byte[] sdp) throws IOException, SdpException {
    SessionDescription sd = sdpFactory.createSessionDescription(new String((byte[]) sdp, "iso8859-1"));
    if (sd.getConnection() != null) {
      sd.getConnection().setAddress("0.0.0.0");
    }

    MediaDescription md = (MediaDescription) sd.getMediaDescriptions(false).get(0);
    if (md.getConnection() != null) {
      md.getConnection().setAddress("0.0.0.0");
    }

    return sd.toString().getBytes("iso8859-1");
  }
}

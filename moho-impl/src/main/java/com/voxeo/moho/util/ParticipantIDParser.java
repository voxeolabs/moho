package com.voxeo.moho.util;

import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParticipantIDParser {
  // format moho://ip:port/<type>/<callid>
  public static Pattern patter = Pattern.compile("moho://(\\S+):(\\S+)/(\\S+)/(\\S+)");

  public static String encode(String raw) {
    byte[] bytes;
    String ret = null;
    try {
      bytes = raw.getBytes("ISO8859-1");
      System.out.println(bytes.length);
      ret = Base64.encodeBytes(bytes, Base64.DONT_BREAK_LINES | Base64.URL_SAFE);
    }
    catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }

    return ret;
  }

  public static String decode(String encoded) {
    String ret = null;
    try {
      ret = new String(Base64.decode(encoded, Base64.DONT_BREAK_LINES | Base64.URL_SAFE), "ISO8859-1");
    }
    catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    return ret;
  }

  public static String[] parseId(String raw) {
    // ip, port, type, id
    Matcher matcher = patter.matcher(raw);
    if (matcher.matches()) {
      return new String[] {matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4)};
    }
    throw new IllegalArgumentException("Illegal ID format:" + raw);
  }

  public static String[] parseEncodedId(String encodedId) {
    String raw = ParticipantIDParser.decode(encodedId);
    return ParticipantIDParser.parseId(raw);
  }
}

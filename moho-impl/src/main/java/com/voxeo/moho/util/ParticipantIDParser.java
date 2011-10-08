package com.voxeo.moho.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParticipantIDParser {
  // format moho://ip:port/<type>/<callid>
  public static Pattern patter = Pattern.compile("moho://(\\S+):(\\S+)/(\\S+)/(\\S+)");

  public static String encode(String raw) {
    // TODO
    return raw;
  }

  public static String decode(String encoded) {
    // TODO
    return encoded;
  }

  public static String[] parseId(String raw) {
    // ip, port, type, id
    Matcher matcher = patter.matcher(raw);
    if (matcher.matches()) {
      return new String[] {matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4)};
    }
    return null;
  }
}

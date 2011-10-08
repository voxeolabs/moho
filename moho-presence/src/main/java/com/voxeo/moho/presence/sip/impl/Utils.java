package com.voxeo.moho.presence.sip.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;


public class Utils {
  
  static Random m_random = new Random();
  
  static final int DEFAULT_MTU = 1500;
  
  private static final char[] TO_HEX   = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

  //JIRA Services-190: The format of the timestamp in <tuple> should be yyyy-MM-dd'T'HH:mm:ss'Z' instead of yyyy-MM-dd'T'HH:mm:ss.S'
  //static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S", Locale.US);
  public static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

  static {
    TIMESTAMP_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US);

  
  /**
   * return udp in the line below:
   * sip:subscriber227RL@127.0.0.1:2222;transport=udp SIP/2.0
   * @param req
   * @return
   */
  public static String getTransport(SipServletRequest req) {
    return ((javax.servlet.sip.SipURI) req.getRequestURI()).getTransportParam();
  }
  
  public static String toHexString(byte b[]) {
    int pos = 0;
    char[] c = new char[b.length * 2];
    for (int i = 0; i < b.length; i++) {
      c[pos++] = TO_HEX[(b[i] >> 4) & 0x0F];
      c[pos++] = TO_HEX[b[i] & 0x0f];
    }
    return new String(c);
  }

  public static String getMD5(String input) {
    MessageDigest messageDigest = null;
    try {
      messageDigest = MessageDigest.getInstance("MD5");
    }
    catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
      return null;
    }
    return toHexString(messageDigest.digest(input.getBytes()));
  }
  
  public static String generate() {
    return Integer.toString(Math.abs(m_random.nextInt()), Character.MAX_RADIX);
  }

  public static int parseCSeqNumber(String cseqHeader) {
    int cseq = -1;
    if (cseqHeader == null) {
      return cseq;
    }
    StringTokenizer cseqToken = new StringTokenizer(cseqHeader.trim());
    if (cseqToken.hasMoreTokens()) {
      try {
        cseq = Integer.parseInt(cseqToken.nextToken());
      }
      catch (NumberFormatException e) {
        cseq = -1;
      }
    }
    return cseq;
  }

  public static String shortenSIPAOR(String aor) {
    if (isNotEmpty(aor)) {
      if (aor.startsWith(SIPConstans.SIP_URI_SCHEME)) {
        return aor.substring(4);
      }
      else if (aor.startsWith(SIPConstans.SIPS_URI_SCHEME)) {
        return aor.substring(5);
      }
    }
    return aor;
  }

  public static boolean isEmpty(String str) {
    return str == null || str.length() == 0;
  }

  public static boolean isNotEmpty(String str) {
    return !isEmpty(str);
  }
  
  public static String getUserOfSIPURI(String uri) {
    if (isNotEmpty(uri) 
        && uri.indexOf(":") > -1 
        && uri.indexOf("@") > -1) {
      return uri.substring(uri.indexOf(":") + 1, uri.indexOf("@")).trim();
    }
    return null;
  }

  public static String getDomainOfSIPURI(String uri) {
    if (isNotEmpty(uri)) {
      int index = uri.indexOf("@");
      if (index >= 0) {
        return uri.substring(index + 1);
      }
      else {
        return null;
      }
    }
    return null;
  }


  public static String longToDate(long updateTime) {
    Date date = new Date(updateTime);
    return DATE_FORMAT.format(date);
  }
  
  public static String longToTimestamp(long updateTime) {
    Date date = new Date(updateTime);
    return TIMESTAMP_FORMAT.format(date);
  }

  public static long dateToLong(String time) throws ParseException {
    Date date = DATE_FORMAT.parse(time);
    return date.getTime();
  }

  public static String wildcardToSql(String keyword) {
    keyword = keyword.replace('*', '%');
    keyword = keyword.replace('?', '_');
    return keyword;
  }

  public static String getOperater(String keyword) {
    if (keyword.contains("<=")) {
      return "<=";
    }
    else if (keyword.contains(">=")) {
      return ">=";
    }
    else if (keyword.contains(">")) {
      return ">";
    }
    else if (keyword.contains("<")) {
      return "<";
    }
    else if (keyword.contains("=")) {
      return "=";
    }
    else {
      return "";
    }
  }
  
  public static boolean isInsideDomain(String domainParam, List<String> domains) {
    boolean isInside = false;
    if (domains != null) {
      isInside = domains.contains(domainParam);
      if (!isInside && !isIpAddress(domainParam)) {
        for (int i = 0; i < domains.size(); i++) {
          String domain = domains.get(i);
          if (isIpAddress(domain)) {
            continue;
          }
          if (domainParam.endsWith("." + domain)) {
            return true;
          }
        }
      }
    }
    return isInside;
  }

  public static String ifLocalHost(String host) throws UnknownHostException {
    if (host.equalsIgnoreCase("localhost")) {
      return InetAddress.getLocalHost().getHostAddress();
    }
    return host;
  }

  public static URI getCleanUri(URI uri) {
    if (uri.isSipURI()) {
      SipURI sipURI = (SipURI) uri.clone();
      Iterator<String> iterator = sipURI.getParameterNames();
      while (iterator != null && iterator.hasNext()) {
        iterator.next();
        iterator.remove();
      }
      return sipURI;
    }
    else {
      return uri;
    }
  }
  
  //for performance, because many eventName.equals(), if invoke this, use "==" to replace "equals"
  public static String staticEventNameStringAddress(String eventName) {
    if (SIPConstans.EVENT_NAME_PRESENCE.equals(eventName)) {
      return SIPConstans.EVENT_NAME_PRESENCE;
    }
    else if (SIPConstans.EVENT_NAME_PRESENCE_WINFO.equals(eventName)) {
      return SIPConstans.EVENT_NAME_PRESENCE_WINFO;
    }
    else if (SIPConstans.EVENT_NAME_REG.equals(eventName)) {
      return SIPConstans.EVENT_NAME_REG;
    }
    else if (SIPConstans.EVENT_NAME_XCAP_DIFF.equals(eventName)) {
      return SIPConstans.EVENT_NAME_XCAP_DIFF;
    }
    else if (SIPConstans.EVENT_NAME_USER_AGENT.equals(eventName)) {
      return SIPConstans.EVENT_NAME_USER_AGENT;
    }
    return eventName;
  }

  public static boolean isIpAddress(String line) {
    if (line == null) {
      return false;
    }
    StringTokenizer st = new StringTokenizer(line, ".");
    if (st.countTokens() != 4) {
      return false;
    }
    int a1 = select(st.nextToken(), 256);
    int a2 = select(st.nextToken(), 256);
    int a3 = select(st.nextToken(), 256);
    int a4 = select(st.nextToken(), 256);
    if (a1 == -1 || a2 == -1 || a3 == -1 || a4 == -1) {
      return false;
    }
    return true;
  }

  public static int select(String line, int index) {
    try {
      int i = Integer.parseInt(line);
      if (i >= 0 && i < index) {
        return i;
      }
      else {
        return -1;
      }
    }
    catch (Exception e) {
      return -1;
    }
  }


  public static void copyFile(File from, File to) throws IOException {
    if (from.exists() && from.canRead() && from.length() > 0) {
      BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(to));
      BufferedInputStream in = new BufferedInputStream(new FileInputStream(from));
      byte[] buf = new byte[512];
      int len = 0;
      while ((len = in.read(buf)) > 0) {
        out.write(buf, 0, len);
      }
      out.close();
      in.close();
    }
  }
  
  public static Properties getPropertiesFileInClassesDir(String path) throws IOException {
    Properties prop = new Properties();
    InputStream is = Utils.class.getResourceAsStream(path);
    prop.load(is);
    return prop;
  }
  
  public static StringBuffer getString(HttpServletRequest req, String content) {
    StringBuffer buf = new StringBuffer("request [");
    buf.append(req.getMethod());
    buf.append(" ");
    buf.append(req.getScheme());
    buf.append(" ");
    buf.append(req.getRequestURI());
    buf.append(" ");
    buf.append(req.getQueryString());
    buf.append("]");
    if (content != null && content.length() > 0) {
      buf.append("\r\n").append(content);
    }
    if (req.getUserPrincipal() != null) {
      buf.append("\r\nPrincipal[" + req.getUserPrincipal().getName() + "]: " + req.getUserPrincipal());
    }
    else {
      buf.append("\r\nPrincipal: null");
    }
    buf.append("\r\n");
    return buf;
  }
}

package com.voxeo.moho.util;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

import javax.management.ObjectName;

import org.apache.log4j.Logger;

public class NetworkUtils {

  private static final Logger log = Logger.getLogger(NetworkUtils.class);

  public static final String MAGIC_COOKIE = "gdg638463";

  public static final byte[] MAGIC_COOKIE_BIN = MAGIC_COOKIE.getBytes();

  private static boolean IPV6_SUPPORTED = false;

  private static boolean IPV4_SUPPORTED = false;

  private static InetAddress LOCAL_ADDRESS = null;

  private static InetAddress LOCAL_ADDRESS_V4 = null;

  private static InetAddress LOCAL_ADDRESS_V6 = null;

  private static final String LOOPBACK_ADDRESS_V4 = "127.0.0.1";

  private static final String LOOPBACK_ADDRESS_V6 = "0:0:0:0:0:0:0:1";

  private static final String LOOPBACK_ADDRESS_V6_ABBR = "::1";

  private static final String ANY_ADDRESS_V4 = "0.0.0.0";

  private static final String ANY_ADDRESS_V6 = "0:0:0:0:0:0:0:0";

  private static final String ANY_ADDRESS_V6_ABBR = "::";

  private static final InetAddress MOCK_ADDRESS_V4_INET = translateAddress("1.1.1.1");

  private static final InetAddress MOCK_ADDRESS_V6_INET = translateAddress("1:1:1:1:1:1:1:1");

  private static final InetAddress LOOPBACK_ADDRESS_V4_INET = translateAddress(LOOPBACK_ADDRESS_V4);

  private static final InetAddress LOOPBACK_ADDRESS_V6_INET = translateAddress(LOOPBACK_ADDRESS_V6);

  private static final InetAddress ANY_ADDRESS_V4_INET = translateAddress(ANY_ADDRESS_V4);

  private static final InetAddress ANY_ADDRESS_V6_INET = translateAddress(ANY_ADDRESS_V6);

  private static final String[] ANY_ADDRESS_LIST = {ANY_ADDRESS_V4, ANY_ADDRESS_V6, ANY_ADDRESS_V6_ABBR,
      "[" + ANY_ADDRESS_V6 + "]", "[" + ANY_ADDRESS_V6_ABBR + "]"};

  private static final String[] LOOPBACK_ADDRESS_LIST = {LOOPBACK_ADDRESS_V4, LOOPBACK_ADDRESS_V6,
      LOOPBACK_ADDRESS_V6_ABBR, "[" + LOOPBACK_ADDRESS_V6 + "]", "[" + LOOPBACK_ADDRESS_V6_ABBR + "]"};

  static {
    resetAddressCache();
  }

  public static boolean isIPAddress(final String addr) {
    return isIPv4Address(addr) || isIPv6Address(addr);
  }

  public static boolean isIPv4Address(final String addr) {
    if (addr == null || addr.length() == 0) {
      return false;
    }
    final String[] st = StringUtils.split(addr, ".");
    if (st.length != 4) {
      return false;
    }
    final int a1 = StringUtils.select(st[0], 256);
    final int a2 = StringUtils.select(st[1], 256);
    final int a3 = StringUtils.select(st[2], 256);
    final int a4 = StringUtils.select(st[3], 256);
    return a1 != -1 && a2 != -1 && a3 != -1 && a4 != -1;
  }

  public static boolean isIPv6Address(final String addr) {
    return !Checker.isEmpty(addr) && sun.net.util.IPAddressUtil.isIPv6LiteralAddress(addr);
  }

  public static boolean isIPv6Possible(final String addr) {
    if (addr == null || addr.length() == 0) {
      return false;
    }
    // return sun.net.util.IPAddressUtil.isIPv6LiteralAddress(addr);
    if (addr.charAt(0) == '[') {
      return true;
    }
    final int index = addr.indexOf(':');
    return index != -1 && addr.indexOf(':', index + 1) != -1;
  }

  public static boolean isIPv6Address(final InetAddress addr) {
    return addr != null && addr instanceof Inet6Address;
  }

  public static boolean isIPv4Address(final InetAddress addr) {
    return addr != null && addr instanceof Inet4Address;
  }

  public static boolean isAnyAddress(final String addr) {
    for (final String s : ANY_ADDRESS_LIST) {
      if (s.equals(addr)) {
        return true;
      }
    }
    return false;
  }

  public static boolean isLoopbackAddress(final String addr) {
    for (final String s : LOOPBACK_ADDRESS_LIST) {
      if (s.equals(addr)) {
        return true;
      }
    }
    return false;
  }

  public static boolean isMulticastAddress(final String addr) {
    try {
      return InetAddress.getByName(addr).isMulticastAddress();
    }
    catch (final UnknownHostException e) {
      return false;
    }
  }

  public static boolean isAvailableIPAddress(final String addr) {
    return isAvailableIPAddress(NetworkUtils.translateAddress(addr));
  }

  public static boolean isAvailableIPAddress(final InetAddress addr) {
    return addr != null && !addr.isLoopbackAddress() && !addr.isAnyLocalAddress() && !addr.isLinkLocalAddress()
        && !addr.isMulticastAddress();
  }

  public static boolean isSupportIPv6() {
    return IPV6_SUPPORTED;
  }

  public static boolean isSupportIPv4() {
    return IPV4_SUPPORTED;
  }

  public static String getPreferAnyAddress() {
    String retval;
    if (isSupportIPv6()) {
      retval = ANY_ADDRESS_V6;
    }
    else {
      retval = ANY_ADDRESS_V4;
    }
    return retval;
  }

  public static String getPreferLoopbackAddress() {
    String retval;
    if (isSupportIPv4()) {
      retval = LOOPBACK_ADDRESS_V4;
    }
    else {
      retval = LOOPBACK_ADDRESS_V6;
    }
    return retval;
  }

  // public static String getPreferLocalAddress(){
  //
  // }

  public static String getLoopbackAddress(final boolean isIPv6) {
    return isIPv6 ? LOOPBACK_ADDRESS_V6 : LOOPBACK_ADDRESS_V4;
  }

  public static List<InetAddress> getAvailableLocalAddresses() {
    final List<InetAddress> retval = new ArrayList<InetAddress>();
    try {
      final Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
      while (networkInterfaces.hasMoreElements()) {
        final Enumeration<InetAddress> addrs = networkInterfaces.nextElement().getInetAddresses();
        while (addrs.hasMoreElements()) {
          final InetAddress addr = addrs.nextElement();
          if (isAvailableIPAddress(addr)) {
            retval.add(0, addr);
          }
        }
      }
    }
    catch (final IOException e) {
      log.warn("NetworkInterfaces: " + getAllNetworkInterfaces(), e);
    }
    return retval;
  }

  public static InetAddress getAvailableLocalAddressForRemote(final InetAddress remote) {
    InetAddress retval = null;
    try {
      final DatagramSocket socket = new DatagramSocket();
      socket.connect(new InetSocketAddress(remote, 5060));
      retval = socket.getLocalAddress();
      if (!isAvailableIPAddress(retval)) {
        retval = null;
      }
      socket.close();
    }
    catch (final Exception e) {
      // ignore
    }
    return retval;
  }

  public static InetAddress getLocalAddress(final InetAddress baseLocalAddress,
      final boolean automaticBaseLocalAddress, final InetAddress comparableAddress) {
    InetAddress retval;
    if (baseLocalAddress == null || baseLocalAddress.isAnyLocalAddress()) {
      if (isIPv4Address(baseLocalAddress)) {
        retval = getLocalAddress(false);
      }
      else if (comparableAddress == null) {
        if (baseLocalAddress == null || automaticBaseLocalAddress) {
          retval = getLocalAddress();
        }
        else {
          retval = getLocalAddress(true);
        }
      }
      else {
        if (isIPv6Address(comparableAddress) && isSupportIPv6()) {
          retval = getLocalAddress(true);
        }
        else if (isIPv4Address(comparableAddress) && isSupportIPv4()) {
          retval = getLocalAddress(false);
        }
        else {
          retval = getLocalAddress();
        }
      }
    }
    else {
      retval = baseLocalAddress;
    }
    return retval;
  }

  public static InetAddress getLocalAddress() {
    if (LOCAL_ADDRESS == null) {
      LOCAL_ADDRESS = getRealtimeLocalAddress();
    }
    return LOCAL_ADDRESS;
  }

  public static InetAddress getLocalAddress(final boolean isIPv6) {
    if (isIPv6) {
      if (LOCAL_ADDRESS_V6 == null) {
        LOCAL_ADDRESS_V6 = getRealtimeV6LocalAddress(true);
      }
      return LOCAL_ADDRESS_V6;
    }
    else {
      if (LOCAL_ADDRESS_V4 == null) {
        LOCAL_ADDRESS_V4 = getRealtimeV4LocalAddress();
      }
      return LOCAL_ADDRESS_V4;
    }
  }

  public static void resetAddressCache() {
    checkProtocolFamily();
    if (LOCAL_ADDRESS_V6 != null) {
      LOCAL_ADDRESS_V6 = getRealtimeV6LocalAddress(true);
    }
    if (LOCAL_ADDRESS_V4 != null) {
      LOCAL_ADDRESS_V4 = getRealtimeV4LocalAddress();
    }
    if (LOCAL_ADDRESS != null) {
      LOCAL_ADDRESS = getRealtimeLocalAddress();
    }
  }

  public static Collection<NetworkInterface> getAllNetworkInterfaces() {
    final Collection<NetworkInterface> retval = new ArrayList<NetworkInterface>();
    try {
      final Enumeration<NetworkInterface> list = NetworkInterface.getNetworkInterfaces();
      while (list.hasMoreElements()) {
        retval.add(list.nextElement());
      }
    }
    catch (final Throwable t) {
      log.warn("", t);
    }
    return retval;
  }

  public static String translateAddress(final InetAddress addr) {
    if (addr == null) {
      return null;
    }
    return normalizeAddress(addr.getHostAddress());
  }

  public static InetAddress translateAddress(final String sAddr) {
    if (sAddr == null) {
      return null;
    }
    try {
      return InetAddress.getByName(sAddr);
    }
    catch (final IOException e) {
      return null;
    }
  }

  public static LazyInetSocketAddress translateAddress(final String sAddr, final int defaultPort) {
    if (sAddr == null) {
      return null;
    }

    String host = sAddr;
    int port = defaultPort;

    if (isIPv6Possible(sAddr)) {
      if (sAddr.charAt(0) == '[') {
        final int index = sAddr.lastIndexOf(']');
        host = sAddr.substring(1, index);
        final int index2 = sAddr.indexOf(':', index);
        if (index2 != -1) {
          port = Integer.parseInt(sAddr.substring(index2 + 1));
        }
      }
      host = normalizeAddress(host);
    }
    else {
      final int index = sAddr.lastIndexOf(':');
      if (index != -1) {
        host = sAddr.substring(0, index);
        port = Integer.parseInt(sAddr.substring(index + 1));
      }
    }

    return new LazyInetSocketAddress(host, port);
  }

  public static String hostportizeAddress(final InetAddress addr) {
    if (addr == null) {
      return null;
    }
    return hostportizeAddress(translateAddress(addr));
  }

  public static String hostportizeAddress(final InetSocketAddress addr) {
    if (addr == null) {
      return null;
    }
    String host;
    if (addr.isUnresolved()) {
      host = normalizeAddress(addr.getHostName());
    }
    else {
      host = NetworkUtils.translateAddress(addr.getAddress());
    }
    return hostportizeAddress(host, addr.getPort());
  }

  public static String hostportizeAddress(final String addr) {
    if (addr == null) {
      return null;
    }
    String retval = addr;
    if (retval.indexOf(':') != -1) {
      if (!retval.startsWith("[")) {
        retval = "[" + retval + "]";
      }
    }
    return retval;
  }

  public static String hostportizeAddress(final String addr, final int port) {
    return NetworkUtils.hostportizeAddress(addr) + ":" + port;
  }

  public static String normalizeAddress(final String addr) {
    if (addr == null || addr.length() == 0) {
      return addr;
    }
    String retval = addr;
    if (retval.charAt(0) == '[') {
      final int index = retval.lastIndexOf(']');
      retval = retval.substring(1, index != -1 ? index : retval.length());
    }
    final int index = retval.lastIndexOf('%');
    if (index != -1) {
      retval = retval.substring(0, index);
    }
    return retval;
  }

  public static String encodeAddress(final String addr) {
    if (addr == null) {
      return null;
    }
    String retval = addr;
    if (retval.indexOf(':') != -1) {
      if (!retval.startsWith("[")) {
        retval = "[" + retval + "]";
      }
      retval = retval.replace(":", "-");
    }
    return retval;
  }

  public static String decodeAddress(final ObjectName oname) {
    if (oname == null) {
      return null;
    }
    String addr = oname.getKeyProperty("address");
    if (addr == null) {
      addr = oname.getKeyProperty("host");
    }
    if (addr == null) {
      return null;
    }
    if (addr.startsWith("[")) {
      return addr.replace("-", ":");
    }
    else {
      return addr.replace("-", ".");
    }
  }

  public static int getFreePort() {
    try {
      final ServerSocket ss = new ServerSocket(0);
      ss.setReuseAddress(true);
      final int port = ss.getLocalPort();
      ss.close();
      return port;
    }
    catch (final Throwable t) {
      throw new RuntimeException(t);
    }
  }

  public static boolean isFreeTCPPort(final int port) {
    try {
      final ServerSocket ss = new ServerSocket(port);
      ss.setReuseAddress(true);
      ss.close();
      return true;
    }
    catch (final Throwable t) {
      t.printStackTrace();
      return false;
    }
  }

  public static void sendRawDAS(final Socket s, final String data) {
    if (data != null) {
      sendRawDAS(s, data.getBytes());
    }
  }

  public static void sendPacket(final byte[] buf, final String host, final int port) {
    try {
      final DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(host), port);
      final DatagramSocket socket = new DatagramSocket();
      socket.send(packet);
      socket.close();
    }
    catch (final Throwable t) {
      throw new RuntimeException(t);
    }
  }

  public static void sendRawDAS(final Socket s, final byte[] data) {
    if (data != null && data.length > 0) {
      try {
        s.getOutputStream().write(data);
        s.getOutputStream().flush();
      }
      catch (final Exception e) {
        // ignore
      }
    }
  }

  public static void sendRawSAS(final OutputStream out, final String data) {
    if (data != null) {
      sendRawSAS(out, data.getBytes());
    }
  }

  public static void sendRawSAS(final OutputStream out, final byte[] data) {
    if (data != null && data.length > 0) {
      try {
        out.write(data);
        out.flush();
      }
      catch (final Exception e) {
        // ignore
      }
    }
  }

  public static MulticastSocket createSocket(final InetAddress groupAddr, final int groupPort, final InetAddress local,
      final int ttl, final boolean join) throws IOException {
    final MulticastSocket socket = new MulticastSocket(groupPort);
    if (local != null) {
      socket.setInterface(local);
    }
    else {
      if (isIPv6Address(groupAddr)) {
        socket.setInterface(getLocalAddress(true));
      }
      else if (isIPv4Address(groupAddr)) {
        socket.setInterface(getLocalAddress(false));
      }
    }
    socket.setTimeToLive(ttl);
    if (join) {
      socket.joinGroup(groupAddr);
    }
    return socket;
  }

  private synchronized static void checkProtocolFamily() {
    if (StringUtils.toBoolean(System.getProperty("java.net.preferIPv6Addresses"))) {
      IPV6_SUPPORTED = true;
      IPV4_SUPPORTED = false;
    }
    else if (StringUtils.toBoolean(System.getProperty("java.net.preferIPv4Stack"))) {
      IPV6_SUPPORTED = false;
      IPV4_SUPPORTED = true;
    }
    // else if (OSUtils.isFamilyWindows() && !SystemUtils.isJava17OrAbove()) {
    // IPV6_SUPPORTED = false;
    // IPV4_SUPPORTED = true;
    // }
    else {
      ServerSocket ss = null;
      try {
        ss = new ServerSocket(0, 1, LOOPBACK_ADDRESS_V6_INET);
        IPV6_SUPPORTED = true;
      }
      catch (final Throwable e) {
        IPV6_SUPPORTED = false;
      }
      try {
        ss = new ServerSocket(0, 1, LOOPBACK_ADDRESS_V4_INET);
        IPV4_SUPPORTED = true;
      }
      catch (final Throwable e) {
        IPV4_SUPPORTED = false;
      }
      if (ss != null) {
        try {
          ss.setReuseAddress(true);
        }
        catch (final Throwable e) {
          // ignore
        }
      }
      try {
        ss.close();
      }
      catch (IOException e) {
        // ignore.
      }
    }
  }

  private static List<InetAddress> getAvailableLocalAddresses(final boolean isIPv6) {
    final List<InetAddress> retval = new ArrayList<InetAddress>();
    for (final InetAddress addr : getAvailableLocalAddresses()) {
      if (isIPv6 && addr instanceof Inet6Address || !isIPv6 && addr instanceof Inet4Address) {
        retval.add(addr);
      }
    }
    return retval;
  }

  private static InetAddress getAvailableLocalAddress(final boolean isIPv6) {
    InetAddress retval = null;
    final List<InetAddress> addrs = getAvailableLocalAddresses(isIPv6);
    if (addrs != null && addrs.size() > 0) {
      retval = addrs.get(0);
    }
    return retval;
  }

  private static InetAddress getRealtimeLocalAddress() {
    InetAddress retval = null;
    try {
      retval = InetAddress.getLocalHost();
    }
    catch (final UnknownHostException e) {
      log.warn("InetAddress.getLocalHost() error: ", e);
    }
    if (!isAvailableIPAddress(retval) && isSupportIPv4()) {
      retval = getAvailableLocalAddressForRemote(MOCK_ADDRESS_V4_INET);
    }
    if (!isAvailableIPAddress(retval) && isSupportIPv6()) {
      retval = getAvailableLocalAddressForRemote(MOCK_ADDRESS_V6_INET);
    }
    if (!isAvailableIPAddress(retval) && isSupportIPv4()) {
      retval = getAvailableLocalAddress(false);
    }
    if (!isAvailableIPAddress(retval) && isSupportIPv6()) {
      retval = getAvailableLocalAddress(true);
    }
    if (!isAvailableIPAddress(retval)) {
      if (isSupportIPv4()) {
        retval = LOOPBACK_ADDRESS_V4_INET;
      }
      else {
        retval = LOOPBACK_ADDRESS_V6_INET;
      }
    }
    else {
      retval = NetworkUtils.translateAddress(NetworkUtils.translateAddress(retval));
    }
    return retval;
  }

  private static InetAddress getRealtimeV6LocalAddress(final boolean loose) {
    InetAddress retval = getAvailableLocalAddressForRemote(MOCK_ADDRESS_V6_INET);
    if (!isAvailableIPAddress(retval)) {
      retval = getAvailableLocalAddress(true);
    }
    if (!isAvailableIPAddress(retval) && loose && isSupportIPv4()) {
      retval = getAvailableLocalAddress(false);
    }
    if (!isAvailableIPAddress(retval)) {
      retval = LOOPBACK_ADDRESS_V6_INET;
    }
    else {
      retval = NetworkUtils.translateAddress(NetworkUtils.translateAddress(retval));
    }
    return retval;
  }

  private static InetAddress getRealtimeV4LocalAddress() {
    InetAddress retval = getAvailableLocalAddressForRemote(MOCK_ADDRESS_V4_INET);
    if (!isAvailableIPAddress(retval)) {
      retval = getAvailableLocalAddress(false);
    }
    if (!isAvailableIPAddress(retval)) {
      retval = LOOPBACK_ADDRESS_V4_INET;
    }
    else {
      retval = NetworkUtils.translateAddress(NetworkUtils.translateAddress(retval));
    }
    return retval;
  }
}

package com.voxeo.moho.util;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class LazyInetSocketAddress implements java.io.Serializable {

  private static final long serialVersionUID = -4168563370269911696L;

  private String hostname = null;

  private InetAddress addr = null;

  private int port;

  private transient InetSocketAddress _socketAddr = null;

  @SuppressWarnings("unused")
  private LazyInetSocketAddress() {
  }

  public LazyInetSocketAddress(final InetSocketAddress addr) {
    if (addr == null) {
      throw new IllegalArgumentException("addr can't be null");
    }
    this.port = addr.getPort();
    if (addr.isUnresolved()) {
      this.hostname = addr.getHostName();
    }
    else {
      this.addr = addr.getAddress();
      this.hostname = NetworkUtils.translateAddress(this.addr);
    }
  }

  public LazyInetSocketAddress(final InetAddress addr, final int port) {
    if (port < -1 || port > 0xFFFF) {
      throw new IllegalArgumentException("port out of range:" + port);
    }
    if (addr == null) {
      throw new IllegalArgumentException("addr can't be null");
    }
    this.addr = addr;
    this.port = port;
  }

  public LazyInetSocketAddress(final String hostname, final int port) {
    if (port < -1 || port > 0xFFFF) {
      throw new IllegalArgumentException("port out of range:" + port);
    }
    if (hostname == null) {
      throw new IllegalArgumentException("hostname can't be null");
    }
    this.hostname = hostname;
    this.port = port;
  }

  private void readObject(final ObjectInputStream s) throws IOException, ClassNotFoundException {
    s.defaultReadObject();
    if (port < -1 || port > 0xFFFF) {
      throw new InvalidObjectException("port out of range:" + port);
    }
    if (hostname == null && addr == null) {
      throw new InvalidObjectException("hostname and addr " + "can't both be null");
    }
  }

  public InetSocketAddress getSocketAddress() {
    if (_socketAddr == null) {
      _socketAddr = new InetSocketAddress(getAddress(), getPort());
    }
    return _socketAddr;
  }

  public final int getPort() {
    return port;
  }

  public final InetAddress getAddress() {
    if (addr == null) {
      addr = NetworkUtils.translateAddress(hostname);
    }
    return addr;
  }

  public final String getHostName() {
    if (hostname == null) {
      hostname = NetworkUtils.translateAddress(addr);
    }
    return hostname;
  }

  public final boolean isUnresolved() {
    return addr == null;
  }

  public String hostportize() {
    return toString();
  }

  @Override
  public String toString() {
    if (getPort() == -1) {
      return getHostName();
    }
    else {
      return NetworkUtils.hostportizeAddress(getHostName()) + ":" + getPort();
    }
  }

  @Override
  public final boolean equals(final Object obj) {
    if (obj == null || !(obj instanceof LazyInetSocketAddress)) {
      return false;
    }
    final LazyInetSocketAddress sockAddr = (LazyInetSocketAddress) obj;
    boolean sameIP = false;

    if (this.addr != null && sockAddr.addr != null) {
      sameIP = this.addr.equals(sockAddr.addr);
    }
    if (!sameIP && this.hostname != null && sockAddr.hostname != null) {
      sameIP = this.hostname.equals(sockAddr.hostname);
    }
    if (!sameIP && this.getAddress() != null && sockAddr.getAddress() != null) {
      sameIP = this.getAddress().equals(sockAddr.getAddress());
    }
    if (!sameIP) {
      sameIP = this.getHostName().equalsIgnoreCase(sockAddr.getHostName());
    }
    return sameIP && this.port == sockAddr.port;
  }

  @Override
  public final int hashCode() {
    if (addr != null) {
      return addr.hashCode() + port;
    }
    if (hostname != null) {
      return hostname.hashCode() + port;
    }
    return port;
  }

}

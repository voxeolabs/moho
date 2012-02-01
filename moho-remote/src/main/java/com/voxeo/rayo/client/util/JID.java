package com.voxeo.rayo.client.util;

/**
 * This interface represent the JID of an entity. The syntax of JID is as
 * follows jid = [ node "@" ] domain [ "/" resource ] domain = fqdn /
 * address-literal fqdn = (sub-domain 1*("." sub-domain)) sub-domain =
 * (internationalized domain label) address-literal = IPv4address / IPv6address
 * valid JID examples: user@example.com/bindid1, example.com ,user@example.com
 * The detail of JID is described in RFC3920.
 * 
 */
public interface JID extends java.lang.Cloneable {
  /**
   * Get the domain identifier.
   * 
   * @return the domain part of the JID.
   */
  String getDomain();

  /**
   * Set the domain identifier.
   * 
   * @param domain
   *          set the domain part.
   */
  void setDomain(String domain);

  /**
   * Get the nod identifier.
   * 
   * @return the node part of the JID.
   */
  String getNode();

  /**
   * Set the nod identifier.
   * 
   * @param node
   *          the node part of the JID.
   */
  void setNode(String node);

  /**
   * Get the resource identifier.
   * 
   * @return the resource part of JID.
   */
  String getResource();

  /**
   * Set the resource identifier.
   * 
   * @param resource
   *          the resource part of JID.
   */
  void setResource(String resource);

  /**
   * Get the bare JID of this JID.
   * 
   * @return the bare JID.
   */
  JID getBareJID();
}

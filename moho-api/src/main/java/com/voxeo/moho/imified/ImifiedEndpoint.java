package com.voxeo.moho.imified;

import com.voxeo.moho.TextableEndpoint;

public interface ImifiedEndpoint extends TextableEndpoint {

  public String getAddress();

  public String getKey();

  public void setKey(String key);

  public String getNetwork();

  public void setNetwork(String network);

  public void setAddress(String address);

  public String getImifiedUserName();

  public void setImifiedUserName(String imifiedUserName);

  public String getImifiedPasswd();

  public void setImifiedPasswd(String imifiedPasswd);
}


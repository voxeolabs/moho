package com.voxeo.moho.remote.sample;

import com.voxeo.moho.remote.AuthenticationCallback;

public class SimpleAuthenticateCallbackImpl implements AuthenticationCallback {
  protected String userName;

  protected String password;

  protected String realm;

  protected String resource;

  public SimpleAuthenticateCallbackImpl(String userName, String password, String realm, String resource) {
    super();
    this.userName = userName;
    this.password = password;
    this.realm = realm;
    this.resource = resource;
  }

  @Override
  public String getUserName() {
    return userName;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getRealm() {
    return realm;
  }

  @Override
  public String getResource() {
    return resource;
  }

}

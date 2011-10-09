package com.voxeo.moho.remote;

@Deprecated
public interface AuthenticationCallback {

  String getUserName();

  String getPassword();
  
  String getResource();

  String getRealm();
}

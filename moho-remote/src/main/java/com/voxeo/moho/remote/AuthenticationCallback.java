package com.voxeo.moho.remote;

public interface AuthenticationCallback {

  String getUserName();

  String getPassword();
  
  String getResource();

  String getRealm();
}

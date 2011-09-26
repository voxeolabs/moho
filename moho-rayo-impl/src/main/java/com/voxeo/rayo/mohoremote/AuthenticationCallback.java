package com.voxeo.rayo.mohoremote;

public interface AuthenticationCallback {

  String getUserName();

  String getPassword();
  
  String getResource();

  String getRealm();
}

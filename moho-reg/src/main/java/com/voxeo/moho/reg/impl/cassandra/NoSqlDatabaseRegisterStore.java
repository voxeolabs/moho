package com.voxeo.moho.reg.impl.cassandra;

import com.voxeo.moho.reg.RegistrarStore;

public abstract class NoSqlDatabaseRegisterStore implements RegistrarStore {

  public void startTx() {
  }

  public void commitTx() {
  }

  public void rollbackTx() {
  }

}

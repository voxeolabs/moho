package com.voxeo.moho;

public interface ExceptionHandler {

  // Returns true if event processing should continue
  public boolean handle(Exception e);
}

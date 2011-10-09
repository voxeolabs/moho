package com.voxeo.moho.presence;

import java.io.Serializable;

import com.voxeo.moho.spi.ExecutionContext;

public interface Resource extends Serializable, Cloneable {
  
  String getUri();
  
  void setExecutionContext(ExecutionContext context);
  
  Resource clone();
}

package com.voxeo.moho.services;

import java.util.Map;

import com.voxeo.moho.spi.ExecutionContext;

public interface Service {

  void init(ExecutionContext context, Map<String, String> properties) throws Exception;

  void destroy();

  String getName();

}

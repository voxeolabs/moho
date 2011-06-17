/**
 * Copyright 2010-2011 Voxeo Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.voxeo.moho.event;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.voxeo.moho.Application;
import com.voxeo.moho.spi.ExecutionContext;
import com.voxeo.moho.spi.ProtocolDriver;
import com.voxeo.moho.spi.SpiFramework;
import com.voxeo.moho.utils.EventListener;

public class ApplicationEventSource extends DispatchableEventSource implements SpiFramework {
  protected Map<String, ProtocolDriver> _driversByProtocol = new HashMap<String, ProtocolDriver>();
  protected Map<String, ProtocolDriver> _driversBySchema = new HashMap<String, ProtocolDriver>();
  protected Application _app;
  
  public ApplicationEventSource(final ExecutionContext context, final Application application) {
    super(context, false);
    if (application instanceof EventListener<?>) {
      addListener((EventListener<?>) application);
    }
    else {
      addObserver(application);
    }
  }

  @Override
  public void registerDriver(String protocol, String className) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    ProtocolDriver driver = createProvider(className);
    registerDriver(driver);
  }
  
  protected void registerDriver(ProtocolDriver driver) {
    String protocol = driver.getProtocolFamily();
    _driversByProtocol.put(protocol, driver);
    for(String schema : driver.getEndpointSchemas()) {
      _driversBySchema.put(schema, driver);
    }
  }
  
  @SuppressWarnings("rawtypes")
  private ProtocolDriver createProvider(String name) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    Class clz = null;
    try {
      clz = this.getClass().getClassLoader().loadClass(name);
    }
    catch (final Throwable t) {
      clz = Thread.currentThread().getContextClassLoader().loadClass(name);
    }
    return (ProtocolDriver) clz.newInstance();
  }

  @Override
  public String[] getProtocolFamilies() {
    Set<String> s = _driversByProtocol.keySet();
    return s.toArray(new String[s.size()]);
  }

  @Override
  public String[] getEndpointSchemas() {
    Set<String> s = _driversBySchema.keySet();
    return s.toArray(new String[s.size()]);
  }

  @Override
  public ProtocolDriver getDriverByProtocolFamily(String protocol) {
    return _driversByProtocol.get(protocol);
  }

  @Override
  public ProtocolDriver getDriverByEndpointSechma(String schema) {
    return _driversBySchema.get(schema);
  }

  @Override
  public Application getApplication() {
    return _app;
  }

  @Override
  public ExecutionContext getExecutionContext() {
    return _context;
  }
}

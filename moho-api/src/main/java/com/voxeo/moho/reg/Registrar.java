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
package com.voxeo.moho.reg;

import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import com.voxeo.moho.Endpoint;

/**
 * The interface encapsulates a Registrar functionality.
 * 
 * @author wchen
 *
 */
public interface Registrar {
  final String STORE_IMPL = "com.voxeo.moho.reg.store.impl";
  final String MAX_EXPIRE = "com.voxeo.moho.reg.expire.max";
  final String DOMAINS = "com.voxeo.moho.reg.domains";
  void init(Properties props);
  void addController(RegistrarController controller);
  void removeController(RegistrarController controller);
  Iterator<RegistrarController> getControllers();
  void doRegister(RegisterEvent event);
  Collection <RegisterEvent.Contact> getContacts(Endpoint aor);
  void destroy();
}

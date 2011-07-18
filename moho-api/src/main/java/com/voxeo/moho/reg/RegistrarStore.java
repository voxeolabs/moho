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
 */package com.voxeo.moho.reg;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.voxeo.moho.Endpoint;
import com.voxeo.moho.reg.RegisterEvent.Contact;

/**
 * This encapsulates the storage (e.g. database) for the {@link Registrar Registrar}.
 * 
 * @author wchen
 *
 */
public interface RegistrarStore {
  void init(Map<String, String> props);
  void startTx();
  void commitTx();
  void rollbackTx();
  void add(Endpoint addr, Contact contact);
  void update(Endpoint addr, Contact contact);
  void remove(Endpoint addr, Contact contact);
  void remove(Endpoint addr);
  Collection<Contact> getContacts(Endpoint addr);
  Iterator<Endpoint> getEndpoints();
  Contact getContact(Endpoint addr, Endpoint contact);
  boolean isExisting(Endpoint addr, Contact contact);
  boolean isExisting(Endpoint addr);
  void destroy();
}

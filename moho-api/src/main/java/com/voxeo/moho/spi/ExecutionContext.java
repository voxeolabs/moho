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

package com.voxeo.moho.spi;

import java.util.concurrent.Executor;

import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.MediaServiceFactory;

public interface ExecutionContext extends ApplicationContext {

  Executor getExecutor();

  Call getCall(String cid);

  void addCall(Call call);

  void removeCall(String id);
  
  MediaServiceFactory getMediaServiceFactory();
  
  void setMediaServiceFactory(MediaServiceFactory factory);
  
  void setFramework(SpiFramework framework);
  
  SpiFramework getFramework();

  void destroy();
}

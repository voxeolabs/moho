/**
 * Copyright 2010 Voxeo Corporation
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

package com.voxeo.moho.queue;

import java.util.Iterator;

import com.voxeo.moho.Call;
import com.voxeo.moho.event.EventSource;

/**
 * This is the base interface for a call queue.
 * 
 * @author wchen
 *
 */
public interface CallQueue extends EventSource {
  
  /**
   * initialize the queue
   */
  void init();
  
  /**
   * uninitialize the queue
   */
  void destroy();
  
  /**
   * @see java.util.Queue#offer(Object)
   */
  boolean offer(Call e);

  /**
   * @see java.util.Queue#peek()
   */
  Call peek();

  /**
   * @see java.util.Queue#poll()
   */
  Call poll();

  /**
   * @see java.util.Collection#isEmpty()
   */
  boolean isEmpty();

  /**
   * @see java.util.Collection#iterator()
   */
  Iterator<Call> iterator();

  /**
   * @see java.util.Queue#remove()
   */
  boolean remove(Call o);

  /**
   * @see java.util.Collection#size()
   */
  int size();

}

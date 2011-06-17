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

package com.voxeo.moho.queue;

import com.voxeo.moho.Call;

/**
 * EnqueueEvent is fired when an item is enqueued into the queue.
 * 
 * @author wchen
 *
 */
public class EnqueueEventImpl extends QueueEventImpl implements EnqueueEvent {
  protected Call _item;

  public EnqueueEventImpl(CallQueue source, Call item) {
    super(source);
  }

  /**
   * @return the item being enqueued.
   */
  public Call getItem() {
    return _item;
  }
}

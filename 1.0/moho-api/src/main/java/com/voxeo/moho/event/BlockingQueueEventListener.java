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

package com.voxeo.moho.event;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.voxeo.utils.Event;
import com.voxeo.utils.EventListener;

public class BlockingQueueEventListener<T extends Event<?>> implements EventListener<T> {

    private BlockingQueue<T> queue;

    public BlockingQueueEventListener() {
        this.queue = new LinkedBlockingQueue<T>();
    }

    public BlockingQueueEventListener(BlockingQueue<T> queue) {
        this.queue = queue;
    }

    public void onEvent(T event) {
        queue.offer(event);
    }

    public BlockingQueue<T> getQueue() {
        return queue;
    }

}

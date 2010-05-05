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

package com.voxeo.moho;

import java.util.concurrent.Future;

import com.voxeo.moho.event.JoinCompleteEvent;

/**
 * A
 * <code>Joint<code> represents the result of the asynchronous <code>join</code>
 * operation on {@link Participant}. Extends the <code>Future</code> interface,
 * so can be used to check if the <code>join</code> operation is complete, to
 * wait for its completion, and to retrieve the result of the operation.
 */
public interface Joint extends Future<JoinCompleteEvent> {

}

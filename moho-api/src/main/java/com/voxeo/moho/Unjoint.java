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

package com.voxeo.moho;

import java.util.concurrent.Future;

import com.voxeo.moho.event.UnjoinCompleteEvent;

/**
 * A
 * <code>Unjoint<code> represents the result of the asynchronous <code>unjoin</code>
 * operation on {@link Participant}. It extends the <code>Future</code>
 * interface, which allows the application to check if the operation is
 * complete, to wait for its completion, and to retrieve the result of the
 * operation.
 */
public interface Unjoint extends Future<UnjoinCompleteEvent> {

}

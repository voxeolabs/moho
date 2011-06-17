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

/**
 * <p>This exception is thrown when application has made a blocking join operation on a call (see below) 
 * and the call is disconnected from the far end before the join is completed.</p>
 * <blockqoute><pre><code>
 *  Call call=...;
 *  try {
 *    call.join().get();
 *  }
 *  catch(HangupException e) { 
 *    // do something
 *  }
 *  </code></pre></blockquote>
 */
public class HangupException extends SignalException {

  private static final long serialVersionUID = -6969149303320819400L;

}

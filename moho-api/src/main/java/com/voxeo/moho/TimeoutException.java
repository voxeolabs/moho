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
 * <p>This exception is thrown when application has made a blocking outbound call (see below) 
 * and the call has timed out without responses.</p>
 * <blockqoute><pre><code>
 *  ApplicationContext context = ...; // context is available when the application is initialized.
 *  CallableEndpoint callee = (CallableEndponint) context.createEndpoint("sip:john@acme.com");
 *  try {
 *    callee.call("sip:doe@acme.com").join().get();
 *  }
 *  catch(TimeoutException e) { 
 *    // do something
 *  }
 *  </code></pre></blockquote>
 */
public class TimeoutException extends SignalException {

  private static final long serialVersionUID = -5967409758136822893L;
}

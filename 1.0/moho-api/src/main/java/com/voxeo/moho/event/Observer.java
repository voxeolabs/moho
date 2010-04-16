/**
 * Copyright 2010 Voxeo Corporation Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.voxeo.moho.event;

/**
 * <p>
 * Observer is a weak-typed listener that can listen on both {@link SignalEvent}
 * and {@link MediaEvent}. Moho supports dispatching event to Observers based
 * on event type and state.
 * </p>
 * <p>
 * The best way to understand Observer is to look at an example.
 * </p>
 * <block><code><pre>
 *   public class MyObserverClass implements Observer {
 *     &#064;State(&quot;greeting&quot;)
 *     public void greetingHandler(InputCompleteEvent input) {
 *       String value = input.getConcept();
 *       if (value.equals(&quot;support&quot;)) {
 *         input.getEventSource().setApplicationState(&quot;support&quot;);
 *         // play support menu and wait for input
 *       }
 *       else if (value.equals(&quot;sales&quot;)) {
 *         input.getEventSource().setApplicationState(&quot;sales&quot;);
 *         // play support menu
 *       }
 *       else {
 *         // replay greeting
 *       }
 *     }
 *     
 *     &#064;State(&quot;sales&quot;)
 *     public void salesHandler(InputCompleteEvent input) {
 *       // handle sales input
 *     }
 *     
 *     &#064;State(&quot;support&quot;)
 *     public void supportHandler(InputCompleteEvent input) {
 *       // handle support input
 *     } 
 *   }
 * </code></pre></block>
 * <p>
 * This example shows how MyObserverClass handles different InputCompleteEvent
 * at different states. <code>greetingHandler</code> is called when an
 * {@link InputCompleteEvent} is fired by the {@link EventSource} and that
 * {@link EventSource}'s application state is "greeting". Similarly,
 * <code>supportHandler</code> and <code>salesHandler</code> are called when
 * {@link InputCompleteEvent} is fired by the {@link EventSource} and that
 * {@link EventSource}'s application state is "support" and "sales"
 * respectively.
 * </p>
 * <p>
 * A method to handle a Moho event:
 * </p>
 * <ul>
 * <li>has to be public.</li>
 * <li>has one and only one parameter whose type is a subtype of
 * {@link com.voxeo.util.Event}.</li>
 * <li>can have one {@link com.voxeo.moho.State} annotation to mark the state
 * name as the pre-condition.</li>
 * </ul>
 * 
 * @author wchen
 * 
 */
public interface Observer {
}

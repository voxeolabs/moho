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

package com.voxeo.moho;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>State annotation is used to mark the pre-condition
 * for a event handler method in an {@link com.voxeo.moho.event.Observer Observer}
 * class.</p>
 * <p>For example, to specify a single state pre-condition, </p>
 * <block><code><pre>
 *   public class MyObserver implements Observer {
 *     &#064;State("myState")
 *     public void myHandler(SignalEvent event) {...}
 *   }
 * </pre></code></block>
 * <p>For example, to specify a single state pre-condition, </p>
 * <block><code><pre>
 *   public class MyObserver implements Observer {
 *     &#064;State("FSM1=myState1","FSM2=myState2")
 *     public void myHandler(SignalEvent event) {...}
 *   }
 * </pre></code></block>
 * 
 * @author wchen
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface State {
    String[] value() default "";
}

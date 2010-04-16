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


public class OutputCompleteEvent extends MediaCompleteEvent {

    private static final long serialVersionUID = 649357868605120409L;

    public enum Cause {
        /** the output is terminated by bargein */
        BARGEIN,
        /** the output is terminated by exceeding its max time allowed */
        TIMEOUT,
        /** the output is terminated by unknown error */
        ERROR,
        /** the output is canceled */
        CANCEL,
        /** the output is completed */
        END,
        /** the output is terminated because the source is disconnected */
        DISCONNECT,
        /** the output is terminated for unknown reason */
        UNKNOWN
    }

    protected Cause _cause;

    public OutputCompleteEvent(EventSource source, Cause cause) {
        super(source);
        _cause = cause;
    }

    public Cause getCause() {
        return _cause;
    }

}

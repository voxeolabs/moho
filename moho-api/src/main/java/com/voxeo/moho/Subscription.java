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

import com.voxeo.moho.event.EventSource;

/**
 * This represents an active event subscription, RFC 3265
 * 
 * @author wchen
 *
 */
public interface Subscription extends EventSource {

    /**
     * Different event type of the subscription
     * 
     * @author wchen
     */
    public enum Type {
        /** RFC 3856 */
        PRESENCE,
        /** BLF */
        DIALOG,
        /**RFC3515 refer event package*/
        REFER
    }

    /**
     * @return the event type
     */
    String getType();

    /**
     * @return the expiration time in secs
     */
    int getExpiration();

    /**
     * renew the subscription
     */
    void renew();
    
    /**
     * @return the address of notifier
     */
    Endpoint getAddress();
    
    /**
     * start the subscription
     */
    void subscribe();
}

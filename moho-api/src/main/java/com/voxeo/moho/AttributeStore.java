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

import java.util.Map;

/**
 * Generic interface for an attribute storage
 * 
 * @author wchen
 *
 */
public interface AttributeStore {

    /**
     * allows the application to retrieve source-specific information
     * @param name
     * @return the named information
     */
    <T> T getAttribute(String name);

    /**
     * allows the application to store source-specific information
     * @param name
     * @param value
     */
    void setAttribute(String name, Object value);

    /**
     * @return Map backing the attribute set
     */
    Map<String, Object> getAttributeMap();

}

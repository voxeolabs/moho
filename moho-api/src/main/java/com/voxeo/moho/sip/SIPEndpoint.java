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

package com.voxeo.moho.sip;

import javax.servlet.sip.Address;
import javax.servlet.sip.SipURI;

import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.TextableEndpoint;

/**
 * SIP address
 * 
 * @author wchen
 *
 */
public interface SIPEndpoint extends CallableEndpoint, TextableEndpoint {

  SipURI getSipURI() throws IllegalArgumentException;

  Address getSipAddress();
  
  boolean isWildCard();
  
}

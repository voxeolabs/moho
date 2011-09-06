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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.sip.SipServletRequest;

import com.voxeo.moho.SignalException;
import com.voxeo.moho.event.MohoHangupEvent;

public class SIPHangupEventImpl extends MohoHangupEvent implements
		SIPHangupEvent {

	protected SipServletRequest _req;

	protected SIPHangupEventImpl(final SIPCall source,
			final SipServletRequest req) {
		super(source);
		_req = req;
	}

	@Override
	public SipServletRequest getSipRequest() {
		return _req;
	}

	@Override
	public synchronized void accept(final Map<String, String> headers)
			throws SignalException {
		this.checkState();
		_accepted = true;
		if (this.source instanceof SIPCallImpl) {
			final SIPCallImpl retval = (SIPCallImpl) this.source;
			retval.doBye(_req, populateHangupHeders(headers));
		}
	}

	@Override
	public synchronized void reject(Reason reason, Map<String, String> headers)
			throws SignalException {
		this.checkState();
		_rejected = true;
		if (this.source instanceof SIPCallImpl) {
			final SIPCallImpl retval = (SIPCallImpl) this.source;
			retval.doBye(_req, populateHangupHeders(headers));
		}
	}

	private Map<String, String> populateHangupHeders(Map<String, String> headers) {

		if (headers == null) {
			headers = new HashMap<String, String>();
		}
        Iterator<String> headerNames = _req.getHeaderNames();
        while(headerNames.hasNext()) {
        	String headerName = headerNames.next();
            headers.put(headerName, _req.getHeader(headerName));
        }
        return headers;
	}
}

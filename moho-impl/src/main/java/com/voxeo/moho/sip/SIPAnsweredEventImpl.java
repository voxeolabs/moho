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

import javax.servlet.sip.SipServletResponse;

import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.MohoAnsweredEvent;

public class SIPAnsweredEventImpl<T extends EventSource> extends
		MohoAnsweredEvent<T> implements SIPAnsweredEvent<T> {

	protected SipServletResponse _res;
	private Map<String, String> headers;

	protected SIPAnsweredEventImpl(final T source, final SipServletResponse res) {
		super(source);
		_res = res;
		Map<String, String> headers = new HashMap<String, String>();
		Iterator<String> it = res.getHeaderNames();
		while (it.hasNext()) {
			String key = it.next();
			headers.put(key, res.getHeader(key));
		}
		if (headers.size() > 0) {
			this.headers = headers;
		}
	}

	@Override
	public Map<String, String> getHeaders() {
		return headers;
	}

	@Override
	public SipServletResponse getSipResponse() {
		return _res;
	}
}

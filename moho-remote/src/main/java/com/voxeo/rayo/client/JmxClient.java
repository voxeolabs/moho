package com.voxeo.rayo.client;

import org.apache.commons.lang.StringUtils;
import org.jolokia.client.J4pClient;
import org.jolokia.client.request.J4pExecRequest;
import org.jolokia.client.request.J4pExecResponse;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pReadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JmxClient {

	private static final Logger log = LoggerFactory.getLogger(JmxClient.class);

	J4pClient client;

	public JmxClient(String hostname, String port) {
		
		if (System.getProperty("hudson.append.ext") != null && !hostname.contains("-ext")) {
			// Small "hack" needed for Hudson functional tests. Otherwise the 
			// tests run on hudson aren't able to access the nodes JMX interfaces
			// as the gateway will return the internal domains for the rayo nodes.
			String[] parts = StringUtils.split(hostname,".");
			parts[0] = parts[0] + "-ext";
			hostname = StringUtils.join(parts,".");
			log.debug("Using hostname: " + hostname);
		}
		
		this.client = new J4pClient("http://" + hostname + ":" + port
				+ "/jolokia");
	}

	public Object jmxValue(String url, String attribute) throws Exception {

		log.debug(String.format("Fetching attribute [%s] from URL [%s]",
				attribute, url));

		J4pReadRequest req = new J4pReadRequest(url, attribute);
		J4pReadResponse resp = client.execute(req);
		return resp.getValue();
	}

	public Object jmxExec(String mbean, String operation, Object... args)
			throws Exception {

		log.debug(String.format(
				"Executing operation [%s] with args [%s] on MBean[%s]",
				operation, args, mbean));

		J4pExecRequest req = new J4pExecRequest(mbean, operation, args);
		J4pExecResponse resp = client.execute(req);
		return resp.getValue();
	}
}

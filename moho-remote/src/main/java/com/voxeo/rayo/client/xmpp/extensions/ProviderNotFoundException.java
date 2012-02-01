package com.voxeo.rayo.client.xmpp.extensions;

@SuppressWarnings("serial")
public class ProviderNotFoundException extends ProviderException {

	public ProviderNotFoundException() {

	}

	public ProviderNotFoundException(String message) {
		super(message);

	}

	public ProviderNotFoundException(Throwable cause) {
		super(cause);

	}

	public ProviderNotFoundException(String message, Throwable cause) {
		super(message, cause);

	}

}

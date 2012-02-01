package com.voxeo.rayo.client.exception;

import com.voxeo.rayo.client.XmppException;

/**
 * <p>This exception is thrown when the dial operation has been 
 * sent but there is no response from the server.</p>
 * 
 * @author martin
 *
 */
@SuppressWarnings("serial")
public class DialTimeoutException extends XmppException {

	public DialTimeoutException() {
		
		super("Dial operation has timed out");
	}
}

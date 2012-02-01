package com.voxeo.rayo.client.listener;

/**
 * Interface for classes that will support stanza listeners
 * 
 * @author martin
 *
 */
public interface StanzaListenerSupport {
	
	/**
	 * Adds a stanza listener to the list of listeners
	 *  
	 * @param listener Stanza listener
	 */
	public void addStanzaListener(StanzaListener listener);
	
	/**
	 * Removes a stanza listener from the list of listeners
	 *  
	 * @param listener Stanza listener
	 */
	public void removeStanzaListener(StanzaListener listener);
}

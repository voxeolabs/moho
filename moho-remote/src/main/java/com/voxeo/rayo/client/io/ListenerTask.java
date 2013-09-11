package com.voxeo.rayo.client.io;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.voxeo.rayo.client.listener.StanzaListener;
import com.voxeo.rayo.client.xmpp.stanza.AbstractXmppObject;
import com.voxeo.rayo.client.xmpp.stanza.Error;
import com.voxeo.rayo.client.xmpp.stanza.IQ;
import com.voxeo.rayo.client.xmpp.stanza.Message;
import com.voxeo.rayo.client.xmpp.stanza.Presence;
import com.voxeo.rayo.client.xmpp.stanza.XmppObject;

/**
 * This task will dispatch an XMPP stanza to all the registered stanza listeners. 
 * 
 * @author martin
 *
 */
public class ListenerTask implements Runnable {

	private Logger log = LoggerFactory.getLogger(UnboundedQueueMessageDispatcher.class);

	private Collection<StanzaListener> listeners = new ConcurrentLinkedQueue<StanzaListener>();
	private XmppObject object;

	public ListenerTask(Collection<StanzaListener> listeners,
					    XmppObject object) {

		this.listeners = listeners;
		this.object = object;
	}

	@Override
	public void run() {

		process((AbstractXmppObject)object);
	}
	
	private void process(XmppObject object) {
		
		log.trace(String.format("Fetched XMPP Object [%s] from the dispatching queue", object));
		for(StanzaListener listener: listeners) {
			if (object instanceof IQ) {
				log.trace(String.format("Invoking listener [%s] onIQ method with IQ id [%s]", listener, object.getId()));
				listener.onIQ((IQ)object);
			} else if (object instanceof Presence) {
				log.trace(String.format("Invoking listener [%s] onPresence method  with presence id [%s]", listener, object.getId()));
				listener.onPresence((Presence)object);
			} else if (object instanceof Message) {
				log.trace(String.format("Invoking listener [%s] onMessage method with message id [%s]", listener, object.getId()));
				listener.onMessage((Message)object);
			} else if (object instanceof Error) {
				log.trace(String.format("Invoking listener [%s] onError method with error id [%s]", listener, object.getId()));
				listener.onError((Error)object);
			}
			log.trace(String.format("Listener [%s] has finished its work", listener));
		}		
	}

}

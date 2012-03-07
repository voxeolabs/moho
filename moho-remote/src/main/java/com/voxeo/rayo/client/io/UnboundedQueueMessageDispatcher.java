package com.voxeo.rayo.client.io;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.voxeo.rayo.client.filter.XmppObjectFilter;
import com.voxeo.rayo.client.listener.StanzaListener;
import com.voxeo.rayo.client.xmpp.stanza.AbstractXmppObject;
import com.voxeo.rayo.client.xmpp.stanza.Error;
import com.voxeo.rayo.client.xmpp.stanza.IQ;
import com.voxeo.rayo.client.xmpp.stanza.Message;
import com.voxeo.rayo.client.xmpp.stanza.Presence;
import com.voxeo.rayo.client.xmpp.stanza.XmppObject;
import com.voxeo.rayo.client.xmpp.stanza.Error.Condition;
import com.voxeo.rayo.client.xmpp.stanza.Error.Type;

/**
 * <p>Implements the {@link MessageDispatcher} interface providing an 
 * implementation based in an unbounded queue and a thread that reads from 
 * the queue and dispatches messages to the different listeners and filters.</p>
 * 
 * @author martin
 *
 */
public class UnboundedQueueMessageDispatcher implements MessageDispatcher {

	private Logger log = LoggerFactory.getLogger(UnboundedQueueMessageDispatcher.class);
	
	private Collection<StanzaListener> stanzaListeners = new ConcurrentLinkedQueue<StanzaListener>();
	private Collection<XmppObjectFilter> filters = new ConcurrentLinkedQueue<XmppObjectFilter>();

	private LinkedBlockingQueue<XmppObject> messagesQueue = new LinkedBlockingQueue<XmppObject>();
	private LinkedBlockingQueue<XmppObject> filtersQueue = new LinkedBlockingQueue<XmppObject>();
	
	private boolean running = true;
	
	/**
	 * Initiates the message dispatcher. When created, the instance will start a 
	 * new thread that will be ready to process incoming messages.
	 */
	public UnboundedQueueMessageDispatcher() {
		
		Runnable listenersTask = new Runnable() {
			
			@Override
			public void run() {
				while(running) {
					XmppObject object = null;
					try {
						object = messagesQueue.poll(30, TimeUnit.SECONDS);
					} catch (InterruptedException e) {}
					
					if (object != null) {
						process(object);
					}
				}
			}
		};
		new Thread(listenersTask).start();
		
		Runnable filterTask = new Runnable() {
			
			@Override
			public void run() {
				while(running) {
					XmppObject object = null;
					try {
						object = filtersQueue.poll(30, TimeUnit.SECONDS);
					} catch (InterruptedException e) {}
					
					if (object != null) {
						filter((AbstractXmppObject)object);
					}
				}
			}
		};
		new Thread(filterTask).start();

	}

	@Override
	public void addStanzaListener(StanzaListener listener) {
		
		stanzaListeners.add(listener);
	}
	
	@Override
	public void removeStanzaListener(StanzaListener listener) {
		
		stanzaListeners.remove(listener);
	}
	
	@Override
    public void addFilter(XmppObjectFilter filter) {

    	filters.add(filter);
    }
    
	@Override
    public void removeFilter(XmppObjectFilter filter) {

    	filters.remove(filter);
    } 
    
    @Override
    public void dispatch(XmppObject object) {

		log.trace(String.format("Dispatching XMPP Object with id [%s] to the dispatching queue", object.getId()));
    	messagesQueue.add(object);
    	filtersQueue.add(object);
    }
    
    private void filter(final AbstractXmppObject object) {

    	log.trace(String.format("Invoking filters on XMPP Object with id [%s]", object.getId()));
    	for (XmppObjectFilter filter: filters) {		    		
    		try {
    			log.trace("Invoking filter " + filter);
    			filter.filter(object);
    			log.trace(String.format("Filter [%s] has finished its work", filter));
			} catch (Exception e) {
				e.printStackTrace();
    			dispatch(new Error(Condition.undefined_condition, Type.cancel, String.format("Error on client filter: %s - %s",e.getClass(),e.getMessage())));  
			}    		
    	}   
    	log.trace(String.format("Done invoking filters", object.getId()));
	}
    
    @Override
    public void reset() {

    	messagesQueue.clear();
    	filters.clear();
    	stanzaListeners.clear();
    }

	private void process(XmppObject object) {
		log.trace(String.format("Fetched XMPP Object [%s] from the dispatching queue", object));
		for(StanzaListener listener: stanzaListeners) {
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
	
	public void shutdown(){
	  running = false;
	}
}

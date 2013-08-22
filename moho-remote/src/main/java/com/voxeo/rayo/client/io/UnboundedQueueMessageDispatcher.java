package com.voxeo.rayo.client.io;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.voxeo.rayo.client.filter.XmppObjectFilter;
import com.voxeo.rayo.client.listener.StanzaListener;
import com.voxeo.rayo.client.xmpp.stanza.XmppObject;

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

	private boolean running = true;

	private ExecutorService executorService;
	
	/**
	 * Initiates the message dispatcher. When created, the instance will start a 
	 * new thread that will be ready to process incoming messages.
	 */
	public UnboundedQueueMessageDispatcher() {
		
	    executorService = Executors.newCachedThreadPool();		
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

		if (running) {
			if (!stanzaListeners.isEmpty()) {
				executorService.execute(new ListenerTask(stanzaListeners, object));
			}
	    	if (!filters.isEmpty()) {
	    		executorService.execute(new FilterTask(this, filters, object));
	    	}
		}
    }
    
    
    @Override
    public void reset() {

    	filters.clear();
    	stanzaListeners.clear();
    }

	
	public void shutdown(){
	  running = false;
	}
}

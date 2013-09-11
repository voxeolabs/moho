package com.voxeo.rayo.client.io;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.voxeo.rayo.client.filter.XmppObjectFilter;
import com.voxeo.rayo.client.xmpp.stanza.AbstractXmppObject;
import com.voxeo.rayo.client.xmpp.stanza.Error;
import com.voxeo.rayo.client.xmpp.stanza.Error.Condition;
import com.voxeo.rayo.client.xmpp.stanza.Error.Type;
import com.voxeo.rayo.client.xmpp.stanza.XmppObject;

/**
 * This task will dispatch an XMPP stanza to all the registered XMPP filters.
 * 
 * @author martin
 *
 */
public class FilterTask implements Runnable {

	private Logger log = LoggerFactory.getLogger(UnboundedQueueMessageDispatcher.class);

	private UnboundedQueueMessageDispatcher parent;
	private Collection<XmppObjectFilter> filters = new ConcurrentLinkedQueue<XmppObjectFilter>();
	private XmppObject object;

	public FilterTask(UnboundedQueueMessageDispatcher parent, 
					  Collection<XmppObjectFilter> filters,
					  XmppObject object) {

		this.parent = parent;
		this.filters = filters;
		this.object = object;
	}

	@Override
	public void run() {

		filter((AbstractXmppObject)object);
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
    			parent.dispatch(new Error(Condition.undefined_condition, Type.cancel, String.format("Error on client filter: %s - %s",e.getClass(),e.getMessage())));  
			}    		
    	}   
    	log.trace(String.format("Done invoking filters", object.getId()));
	}

}

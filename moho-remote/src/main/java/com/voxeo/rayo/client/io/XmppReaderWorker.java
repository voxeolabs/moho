package com.voxeo.rayo.client.io;

import java.io.Reader;
import java.net.SocketException;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.voxeo.rayo.client.XmppConnectionListener;
import com.voxeo.rayo.client.auth.AuthenticationListener;
import com.voxeo.rayo.client.filter.XmppObjectFilter;
import com.voxeo.rayo.client.filter.XmppObjectFilterSupport;
import com.voxeo.rayo.client.listener.StanzaListener;
import com.voxeo.rayo.client.listener.StanzaListenerSupport;
import com.voxeo.rayo.client.util.XmppObjectParser;
import com.voxeo.rayo.client.xmpp.stanza.Error;
import com.voxeo.rayo.client.xmpp.stanza.IQ;
import com.voxeo.rayo.client.xmpp.stanza.Message;
import com.voxeo.rayo.client.xmpp.stanza.Presence;
import com.voxeo.rayo.client.xmpp.stanza.XmppObject;
import com.voxeo.rayo.client.xmpp.stanza.Error.Condition;
import com.voxeo.rayo.client.xmpp.stanza.Error.Type;
import com.voxeo.rayo.client.xmpp.stanza.sasl.Challenge;
import com.voxeo.rayo.client.xmpp.stanza.sasl.Success;

public class XmppReaderWorker implements Runnable, StanzaListenerSupport, XmppObjectFilterSupport {
	
	private Logger log = LoggerFactory.getLogger(XmppReaderWorker.class);
	
	private XmlPullParser parser;
	private String connectionId;
	
	private boolean done;
	
	private Reader reader;
	
	private Collection<XmppConnectionListener> listeners = new ConcurrentLinkedQueue<XmppConnectionListener>();
	private Collection<AuthenticationListener> authListeners = new ConcurrentLinkedQueue<AuthenticationListener>();
	
	private MessageDispatcher messageDispatcher;
	
	public XmppReaderWorker() {
		
		messageDispatcher = new UnboundedQueueMessageDispatcher();
	}
	
	@Override
	public void run() {

		parse();
	}
	
	public void addXmppConnectionListener(XmppConnectionListener listener) {

		listeners.add(listener);
	}
	
	public void removeXmppConnectionListener(XmppConnectionListener listener) {

		listeners.remove(listener);
	}
	
	public void addStanzaListener(StanzaListener listener) {
		
		messageDispatcher.addStanzaListener(listener);
	}
	
	public void removeStanzaListener(StanzaListener listener) {
		
		messageDispatcher.removeStanzaListener(listener);
	}
	
    public void addAuthenticationListener(AuthenticationListener authListener) {

    	authListeners.add(authListener);
    }
    
    public void removeAuthenticationListener(AuthenticationListener authListener) {
    	
    	authListeners.remove(authListener);
    }
    
    public void addFilter(XmppObjectFilter filter) {

    	messageDispatcher.addFilter(filter);
    }
    
    public void removeFilter(XmppObjectFilter filter) {

    	messageDispatcher.removeFilter(filter);
    }  
	
    public void resetParser(Reader reader) {
    	
    	log("Reseting parser");
        try {
        	this.reader = reader;
            parser = new MXParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            parser.setInput(reader);
        }
        catch (XmlPullParserException xppe) {
            xppe.printStackTrace();
        }
    }
    
    /**
     * Parse top-level packets in order to process them further.
     *
     * @param thread the thread that is being used by the reader to parse incoming packets.
     */
    private void parse() {
    	
        try {
            int eventType = parser.getEventType();            
            do {
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.getName().equals("message")) {
                    	final Message message = XmppObjectParser.parseMessage(parser);
                    	log(message);
                    	messageDispatcher.dispatch(message);
                    } else if (parser.getName().equals("iq")) {
                    	final IQ iq = XmppObjectParser.parseIQ(parser);
                    	if (iq.hasChild("error")) {
                    		handleError(iq.getError());
                    	}
                    	log(iq);
                    	messageDispatcher.dispatch(iq);
                    } else if (parser.getName().equals("presence")) {
                    	final Presence presence = XmppObjectParser.parsePresence(parser);
                    	log(presence);
                    	messageDispatcher.dispatch(presence);
                    }
                    // We found an opening stream. Record information about it, then notify
                    // the connectionID lock so that the packet reader startup can finish.
                    else if (parser.getName().equals("stream")) {
                        // Ensure the correct jabber:client namespace is being used.
                        if ("jabber:client".equals(parser.getNamespace(null))) {
                            // Get the connection id.
                            for (int i=0; i<parser.getAttributeCount(); i++) {
                                if (parser.getAttributeName(i).equals("id")) {
                                    // Save the connectionID
                                	connectionId = parser.getAttributeValue(i);
                                	log("Received new connection stream with id: " + connectionId);
                                    if (!"1.0".equals(parser.getAttributeValue("", "version"))) {
                                        // Notify that a stream has been opened if the
                                        // server is not XMPP 1.0 compliant otherwise make the
                                        // notification after TLS has been negotiated or if TLS
                                        // is not supported
                                    	connectionEstablished();
                                    }
                                }
                                else if (parser.getAttributeName(i).equals("from")) {

                                }
                            }
                        }
                    }
                    else if (parser.getName().equals("error")) {
                    	Error error = XmppObjectParser.parseError(parser);
                    	log(error);
                    	handleError(error);
                    }
                    else if (parser.getName().equals("features")) {
                    	log("Received features");
                    	parseFeatures(parser);
                    }
                    else if (parser.getName().equals("proceed")) {

                    }
                    else if (parser.getName().equals("failure")) {

                    }
                    else if (parser.getName().equals("challenge")) {
                    	final Challenge challenge = new Challenge().setText(parser.nextText());
                    	for (final AuthenticationListener listener: authListeners) {
	                	    listener.authChallenge(challenge);
                    	}
                    }
                    else if (parser.getName().equals("success")) {
                    	final Success success = new Success().setText(parser.nextText());
                    	log(success);
                    	for (final AuthenticationListener listener: authListeners) {
                    	    listener.authSuccessful(success);
                    	}

                    	// We now need to bind a resource for the connection
                        // Open a new stream and wait for the response
                    	for (final XmppConnectionListener listener: listeners) {
		            	    listener.connectionReset(connectionId);									
                    	}

                        // Reset the state of the parser since a new stream element is going
                        // to be sent by the server
                    	resetParser(reader);                    	
                    	
                    }
                    else if (parser.getName().equals("compressed")) {

                    }
                }
                else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.getName().equals("stream")) {
                        // Disconnect the connection
            	    	for (final XmppConnectionListener listener: listeners) {
		            	    listener.connectionFinished(connectionId);									
            	    	}
                    }
                }
                if (parser == null) {
                	log("Parser is null. Exiting.");
                	done = true;
                } else {
                	eventType = parser.next();
                }
            } while (!done && eventType != XmlPullParser.END_DOCUMENT);
        } catch (SocketException se) {
        	if (!done) {
            	se.printStackTrace();
                handleError(new Error(Condition.gone, Type.cancel, se.getMessage()));        		
        	}
        } catch (Exception e) {        	
        	e.printStackTrace();    
        	handleError(new Error(Condition.undefined_condition, Type.cancel, e.getMessage()));
        }
    }

	private void parseFeatures(XmlPullParser parser) throws Exception {
    	
        boolean startTLSReceived = false;
        boolean startTLSRequired = false;
        boolean done = false;
        while (!done) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("starttls")) {
                    startTLSReceived = true;
                }
                else if (parser.getName().equals("mechanisms")) {
                	log("Received mechanisms");
                    // The server is reporting available SASL mechanisms. Store this information
                    // which will be used later while logging (i.e. authenticating) into
                    // the server
                	final Collection<String> mechanisms = XmppObjectParser.parseMechanisms(parser);
        	    	for (final AuthenticationListener listener: authListeners) {
                	    listener.authSettingsReceived(mechanisms);
        	    	}
                }
                else if (parser.getName().equals("bind")) {
                	log("Received bind");                	
        	    	for (final AuthenticationListener listener: authListeners) {
        	    		listener.authBindingRequired();
        	    	}
                }
                else if (parser.getName().equals("session")) {
                	log("Received session");
        	    	for (final AuthenticationListener listener: authListeners) {
                	    listener.authSessionsSupported();
        	    	}
                }
                else if (parser.getName().equals("compression")) {
                    // The server supports stream compression

                }
                else if (parser.getName().equals("register")) {

                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("starttls")) {
                    // Confirm the server that we want to use TLS

                }
                else if (parser.getName().equals("required") && startTLSReceived) {
                    startTLSRequired = true;
                }
                else if (parser.getName().equals("features")) {
                    done = true;
                }
            }
        }
        
        //TODO: Lots of stuff to handle here. Code based in Packet reader from Smack
        
        // Release the lock after TLS has been negotiated or we are not insterested in TLS
        if (!startTLSReceived || (startTLSReceived && !startTLSRequired)) {
        	connectionEstablished();
        }
    }
    
    private void connectionEstablished() {
    	
    	if (connectionId != null) {
	    	for (final XmppConnectionListener listener: listeners) {
        	    listener.connectionEstablished(connectionId);									
	    	}
    	}
    }
    
    private void connectionFinished() {
    	
    	if (connectionId != null) {
	    	for (final XmppConnectionListener listener: listeners) {
        	    listener.connectionFinished(connectionId);									
	    	}
    	}
    }
    
    void handleError(Error e) {
    	
    	messageDispatcher.dispatch(e);
    }

	public void setDone(boolean done) {
		
		this.done = done;
		connectionFinished();
	}
	
	public void reset() {
		
		resetParser(reader);
		cleanListeners();
	}

	public void shutdown() {
		
		reader = null;
		parser = null;
		connectionId = null;
		cleanListeners();
	}
	
	private void cleanListeners() {
		
		listeners.clear();
		authListeners.clear();
		messageDispatcher.reset();
	};
    
    public String getConnectionId() {
    	
    	return connectionId;
    }
    
    private void log(XmppObject object) {
    	
    	log(object.toString());
    }
    
    private void log(String value) {
    	
    	log.debug(String.format("[IN ] [%s]", value));
    }
}

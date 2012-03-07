package com.voxeo.rayo.client;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.media.mscontrol.join.Joinable;

import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rayo.core.AcceptCommand;
import com.rayo.core.AnswerCommand;
import com.rayo.core.CallCommand;
import com.rayo.core.CallRejectReason;
import com.rayo.core.DialCommand;
import com.rayo.core.DtmfCommand;
import com.rayo.core.HangupCommand;
import com.rayo.core.JoinCommand;
import com.rayo.core.JoinDestinationType;
import com.rayo.core.OfferEvent;
import com.rayo.core.RedirectCommand;
import com.rayo.core.RejectCommand;
import com.rayo.core.UnjoinCommand;
import com.rayo.core.verb.Ask;
import com.rayo.core.verb.Choices;
import com.rayo.core.verb.Conference;
import com.rayo.core.verb.HoldCommand;
import com.rayo.core.verb.Input;
import com.rayo.core.verb.InputMode;
import com.rayo.core.verb.MuteCommand;
import com.rayo.core.verb.Output;
import com.rayo.core.verb.Record;
import com.rayo.core.verb.RecordPauseCommand;
import com.rayo.core.verb.RecordResumeCommand;
import com.rayo.core.verb.Say;
import com.rayo.core.verb.SeekCommand;
import com.rayo.core.verb.SpeedDownCommand;
import com.rayo.core.verb.SpeedUpCommand;
import com.rayo.core.verb.Ssml;
import com.rayo.core.verb.StopCommand;
import com.rayo.core.verb.Transfer;
import com.rayo.core.verb.UnholdCommand;
import com.rayo.core.verb.UnmuteCommand;
import com.rayo.core.verb.VerbRef;
import com.rayo.core.verb.VolumeDownCommand;
import com.rayo.core.verb.VolumeUpCommand;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.rayo.client.auth.AuthenticationListener;
import com.voxeo.rayo.client.exception.DialTimeoutException;
import com.voxeo.rayo.client.filter.XmppObjectFilter;
import com.voxeo.rayo.client.listener.RayoMessageListener;
import com.voxeo.rayo.client.listener.StanzaListener;
import com.voxeo.rayo.client.registry.Call;
import com.voxeo.rayo.client.registry.CallsRegistry;
import com.voxeo.rayo.client.verb.ClientPauseCommand;
import com.voxeo.rayo.client.verb.ClientResumeCommand;
import com.voxeo.rayo.client.verb.RefEvent;
import com.voxeo.rayo.client.xmpp.extensions.Extension;
import com.voxeo.rayo.client.xmpp.stanza.IQ;
import com.voxeo.rayo.client.xmpp.stanza.Ping;
import com.voxeo.rayo.client.xmpp.stanza.Presence;
import com.voxeo.rayo.client.xmpp.stanza.Stanza;
import com.voxeo.rayo.client.xmpp.stanza.Presence.Show;
import com.voxeo.rayo.client.xmpp.stanza.Presence.Type;

/**
 * This class servers as a client to the Rayo XMPP platform.
 * 
 * @author martin
 *
 */
public class RayoClient {

	private Logger logger = LoggerFactory.getLogger(RayoClient.class);
	
	protected final XmppConnection connection;
	public static final String DEFAULT_RESOURCE = "voxeo";

	private CallsRegistry callRegistry = new CallsRegistry();
	
	private String rayoServer;
	
	private ReentrantReadWriteLock connectionLock = new ReentrantReadWriteLock();
	
	private Timer pingTimer = null;
	
	/**
	 * Creates a new client object. This object will be used to interact with an Rayo server.
	 * 
	 * @param server Rayo Server that this client will be connecting to
	 */
	public RayoClient(String xmppServer, String rayoServer) {

		this(xmppServer, null, rayoServer);
	}
	
	/**
	 * Creates a new client object that will use the specified port number. 
	 * This object will be used to interact with an Rayo server.
	 * 
	 * @param server Server that this client will be connecting to
	 * @param port Port number that the server is listening at
	 */
	public RayoClient(String xmppServer, Integer port, String rayoServer) {

		connection = new SimpleXmppConnection(xmppServer, port);
		this.rayoServer = rayoServer;
	}
	
	/**
	 * Creates a Rayo Client using the given XMPP connection
	 * 
	 * @param connection XMPP connection that will be used
	 * @param rayoServer Rayo Sever to connect this Rayo client to
	 */
	public RayoClient(XmppConnection connection, String rayoServer) {

		this.connection = connection;
		this.rayoServer = rayoServer;
	}
	
	/**
	 * Connects and authenticates into the Rayo Server. By default it will use the resource 'voxeo'.
	 * 
	 * @param username Rayo username
	 * @param password Rayo password
	 * 
	 * @throws XmppException If the client is not able to connect or authenticate into the Rayo Server
	 */
	public void connect(String username, String password) throws XmppException {
		
		connect(username, password, DEFAULT_RESOURCE);
	}

	/**
	 * Connects and authenticates into the Rayo Server. By default it will use the resource 'voxeo'.
	 * 
	 * @param username Rayo username
	 * @param password Rayo password
	 * @param resource Resource that will be used in this communication
	 * 
	 * @throws XmppException If the client is not able to connect or authenticate into the Rayo Server
	 */
	public void connect(String username, String password, String resource) throws XmppException {
		connect(username, password, resource, 5);
	}

	/**
	 * Connects and authenticates into the Rayo Server. By default it will use the resource 'voxeo'.
	 * 
	 * @param username Rayo username
	 * @param password Rayo password
	 * @param resource Resource that will be used in this communication
	 * 
	 * @throws XmppException If the client is not able to connect or authenticate into the Rayo Server
	 */
	public void connect(String username, String password, String resource, int timeout) throws XmppException {
		
		Lock lock = connectionLock.writeLock();
		lock.lock();
		
		if (connection.isConnected()) {
			try {
				disconnect();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}		
		
		try {
			logger.info("Connecting Rayo client XMPP Connection");
			if (!connection.isConnected()) {
				connection.connect(timeout);
				connection.login(username, password, resource, timeout);
				
				connection.addStanzaListener(new RayoMessageListener("offer") {
					
					@Override
					@SuppressWarnings("rawtypes")
					public void messageReceived(Object object) {
						
						//TODO: Stanza should have methods to fetch the JID node name, domain, etc.
						Stanza stanza = (Stanza)object;
						int at = stanza.getFrom().indexOf('@');
						String callId = stanza.getFrom().substring(0, at);
						String domain = stanza.getFrom().substring(at+1);
						if (domain.contains(":")) {
							domain = domain.substring(0, domain.indexOf(':'));
						}
						Call call = new Call(callId, domain);
						callRegistry.registerCall(callId, call);
					}
				});
				connection.addStanzaListener(new RayoMessageListener("end") {
					
					@Override
					@SuppressWarnings("rawtypes")
					public void messageReceived(Object object) {
						
						//TODO: Stanza should have methods to fetch the JID node name, domain, etc.
						Stanza stanza = (Stanza)object;
						int at = stanza.getFrom().indexOf('@');
						String callId = stanza.getFrom().substring(0, at);
						callRegistry.unregisterCal(callId);
					}
				});	
				
				broadcastAvailability();
				
				TimerTask pingTask = new TimerTask() {
					
					@Override
					public void run() {
		
						ping();
					}
				};
				pingTimer = new Timer();
				pingTimer.schedule(pingTask, 5000, 30000);
				
				connection.addStanzaListener(new RayoMessageListener("ping") {
					
					@Override
					public void messageReceived(Object object) {
		
						IQ iq = (IQ)object;
						if (!iq.isError()) {
							// pong
							try {
								sendIQ(iq.result());
							} catch (XmppException e) {
								e.printStackTrace();
							}
						}
					}
				});
			} else {
				logger.error("Trying to connect while the old XMPP connection is active. Please, disconnect first");
			}
			logger.info("Rayo client is now connected");
		} catch (XmppException xe) {
			logger.error("Error while trying to opean an XMPP connection");
			xe.printStackTrace();
			throw xe;
		} catch (Exception e) {
			logger.error("Error while trying to opean an XMPP connection");
			e.printStackTrace();
			throw new XmppException(e.getMessage());
		} finally {
			lock.unlock();
		}
	}

	public void setAvailable(boolean status) throws XmppException {
		
		if (status == true) {
			broadcastAvailability();
		} else {
			broadcastUnavailability();
		}
	}
	
	public void setStatus(Show status) throws XmppException {
		
		Presence presence = new Presence()
			.setId(UUID.randomUUID().toString())
			.setFrom(connection.getUsername() + "@" + connection.getServiceName() + "/" + connection.getResource())
			.setTo(rayoServer)
			.setShow(status);
		
		connection.send(presence);
	}
	
	private void broadcastAvailability() throws XmppException {

		Presence presence = new Presence()
			.setId(UUID.randomUUID().toString())
			.setShow(Show.chat);
		connection.send(presence);
		
		presence = new Presence()
			.setId(UUID.randomUUID().toString())
			.setFrom(connection.getUsername() + "@" + connection.getServiceName() + "/" + connection.getResource())
			.setTo(rayoServer)
			.setShow(Show.chat);
		connection.send(presence);

	}

	private void broadcastUnavailability() throws XmppException {
		
		Presence presence = new Presence()
			.setId(UUID.randomUUID().toString())
			.setFrom(connection.getUsername() + "@" + connection.getServiceName() + "/" + connection.getResource())
			.setTo(rayoServer)
			.setType(Type.unavailable);
		connection.send(presence);

		presence = new Presence()
			.setId(UUID.randomUUID().toString())
			.setType(Type.unavailable);
		connection.send(presence);
	}

	/**
	 * Adds a callback class to listen for events on all the incoming stanzas.
	 * 
	 * @param listener Stanza Callback.
	 */
	public void addStanzaListener(StanzaListener listener) {
		
		Lock lock = connectionLock.readLock();
		lock.lock();
		try {
			connection.addStanzaListener(listener);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Removes a stanza listener
	 * 
	 * @param listener Stanza Callback to be removed
	 */
	public void removeStanzaListener(StanzaListener listener) {

		Lock lock = connectionLock.readLock();
		lock.lock();
		try {
			connection.removeStanzaListener(listener);
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * Adds a callback class to listen for authentication events.
	 * 
	 * @param listener Callback.
	 */
	public void addAuthenticationListener(AuthenticationListener listener) {
		
		Lock lock = connectionLock.readLock();
		lock.lock();
		try {
			connection.addAuthenticationListener(listener);
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * Adds an XMPP filter
	 * 
	 * @param filter Filter object to be added
	 */
	public void addFilter(XmppObjectFilter filter) {
		
		Lock lock = connectionLock.readLock();
		lock.lock();
		try {
			connection.addFilter(filter);
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * Removes an XMPP filter
	 * 
	 * @param filter Filter object to be removed
	 */
	public void removeFilter(XmppObjectFilter filter) {

		Lock lock = connectionLock.readLock();
		lock.lock();
		try {
			connection.removeFilter(filter);
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * Disconnects this client connection from the Rayo server
	 * 
	 */
	public void disconnect() throws XmppException {
		
		Lock lock = connectionLock.writeLock();
		lock.lock();
		try {
			logger.info("Disconnecting Rayo client XMPP Connection");
			if (connection.isConnected()) {
				broadcastUnavailability();
				
				connection.disconnect();
			}
		} finally {
			logger.info("Rayo Client XMPP Connection has been disconnected");
			lock.unlock();
			pingTimer.cancel();
			pingTimer = null;
		}
	}
	
	
	/**
	 * <p>Waits for an Offer Event. Shortcut method to wait for an incoming call.</p>
	 * 
	 * @return OfferEvent Offer event that has been received
	 * 
	 * @throws XmppException If there is any problem waiting for offer event
	 */
	public OfferEvent waitForOffer() throws XmppException {
	
		return waitForOffer(null);
	}
	
	/**
	 * <p>Waits for an Offer Event. Shortcut method to wait for an incoming call.</p>
	 * 
	 * @timeout Timeout 
	 * @return OfferEvent Offer event that has been received
	 * 
	 * @throws XmppException If there is any problem waiting for offer event
	 */
	public OfferEvent waitForOffer(Integer timeout) throws XmppException {
		
		final StringBuilder callId = new StringBuilder();
		RayoMessageListener tempListener = new RayoMessageListener("offer") {
			
			@Override
			@SuppressWarnings("rawtypes")
			public void messageReceived(Object object) {
				
				Stanza stanza = (Stanza)object;
				callId.append(stanza.getFrom().substring(0, stanza.getFrom().indexOf('@')));
			}
		};
		addStanzaListener(tempListener);
		try {
			OfferEvent stanza = waitFor("offer", OfferEvent.class, timeout);
			
			OfferEvent offer = new OfferEvent(callId.toString());
			offer.setTo(stanza.getTo());
			offer.setFrom(stanza.getFrom());
			offer.setHeaders(stanza.getHeaders());
			
			return offer;
		} finally {
			removeStanzaListener(tempListener);
		}
	}
	
	/**
	 * <p>Waits for an Rayo message. This is a blocking call and therefore should be used carefully. 
	 * When invoked, the invoking thread will block until it receives the specified Rayo 
	 * message.</p>
	 * 
	 * @param rayoMessage Rayo message that the invoking thread will be waiting for
	 *  
	 * @return Object The first Rayo messaging received that matches the specified message name
	 * 
	 * @throws XmppException If there is any problem waiting for the message
	 */
	public Object waitFor(String rayoMessage) throws XmppException {
		
		Lock lock = connectionLock.readLock();
		lock.lock();
		try {
			Extension extension = (Extension)connection.waitForExtension(rayoMessage);
			return extension.getObject();
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * <p>Waits for an Rayo message. This is a blocking call and therefore should be used carefully. 
	 * When invoked, the invoking thread will block until it receives the specified Rayo 
	 * message.</p>
	 * 
	 * @param rayoMessage Rayo message that the invoking thread will be waiting for
	 * @param clazz Class to cast the returning object to
	 *  
	 * @return T The first Rayo messaging received that matches the specified message name
	 * 
	 * @throws XmppException If there is any problem waiting for the message
	 */
	@SuppressWarnings("unchecked")
	public <T> T waitFor(String rayoMessage, Class<T> clazz) throws XmppException {

		Lock lock = connectionLock.readLock();
		lock.lock();
		try {
			Extension extension = (Extension)connection.waitForExtension(rayoMessage);
			return (T)extension.getObject();
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * <p>Waits for an Rayo message. This is a blocking call but uses a timeout to specify 
	 * the amount of time that the connection will wait until the specified message is received.
	 * If no message is received during the specified timeout then a <code>null</code> object 
	 * will be returned.</p>
	 * 
	 * @param rayoMessage Rayo message that the invoking thread will be waiting for
	 * @param timeout Timeout that will be used when waiting for an incoming Rayo message
	 *  
	 * @return Object The first Rayo messaging received that matches the specified message name 
	 * or <code>null</code> if no message is received during the specified timeout
	 * 
	 * @throws XmppException If there is any problem waiting for the message
	 */
	public Object waitFor(String extensionName, int timeout) throws XmppException {

		Lock lock = connectionLock.readLock();
		lock.lock();
		try {
			Extension extension = (Extension)connection.waitForExtension(extensionName, timeout);
			return extension.getObject();
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * <p>Waits for an Rayo message. This is a blocking call but uses a timeout to specify 
	 * the amount of time that the connection will wait until the specified message is received.
	 * If no message is received during the specified timeout then a <code>null</code> object 
	 * will be returned.</p>
	 * 
	 * @param rayoMessage Rayo message that the invoking thread will be waiting for
	 * @param clazz Class to cast the returning object to
	 * @param timeout Timeout that will be used when waiting for an incoming Rayo message
	 *  
	 * @return Object The first Rayo messaging received that matches the specified message name 
	 * or <code>null</code> if no message is received during the specified timeout
	 * 
	 * @throws XmppException If there is any problem waiting for the message
	 */
	@SuppressWarnings("unchecked")
	public <T> T waitFor(String extensionName, Class<T> clazz, Integer timeout) throws XmppException {

		Lock lock = connectionLock.readLock();
		lock.lock();
		try {
			Extension extension = (Extension)connection.waitForExtension(extensionName, timeout);
			return (T)extension.getObject();
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * Answers the call with the id specified as a parameter. 
	 * 
	 * @param callId Id of the call that will be answered
	 * 
	 * @return IQ Resulting IQ
	 * 
	 * @throws XmppException If there is any issue while answering the call
	 */
	public IQ answer(String callId) throws XmppException {
		
		return answer(callId, new AnswerCommand());	
	}
	
	
	/**
	 * Answers the call with the id specified as a parameter. 
	 * 
	 * @param callId Id of the call that will be answered
	 * @param command Answer command
	 * 
	 * @return IQ Resulting IQ
	 * 
	 * @throws XmppException If there is any issue while answering the call
	 */
	public IQ answer(String callId, AnswerCommand command) throws XmppException {
		
		IQ iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom())
			.setTo(buildTo(callId))
			.setChild(Extension.create(command));
		return sendIQ(iq);		
	}
	
	/**
	 * Accepts the call with the id specified as a parameter. 
	 * 
	 * @param callId Id of the call that will be accepted
	 * @return IQ Resulting IQ
	 * @throws XmppException If there is any issue while accepting the call
	 */
	public IQ accept(String callId) throws XmppException {
		
		return accept(callId, new AcceptCommand());	
	}	
	
	
	/**
	 * Accepts the call with the id specified as a parameter. 
	 * 
	 * @param callId Id of the call that will be accepted
	 * @param command Accept command
	 * @return IQ Resulting IQ
	 * @throws XmppException If there is any issue while accepting the call
	 */
	public IQ accept(String callId, AcceptCommand command) throws XmppException {
		
		return command(command, callId);	
	}	
	
	/**
	 * Rejects the latest call that this connection has received from the Rayo server
	 * 
	 * @param callId Id of the call
	 * @return IQ Resulting IQ
	 * 
	 * @throws XmppException If there is any issue while rejecting the call
	 */
	public IQ reject(String callId) throws XmppException {

		return reject(CallRejectReason.DECLINE, callId);
	}
	
	
	/**
	 * Rejects a call id
	 * 
	 * @param reject Reject command
	 * @param callId Id of the call
	 * @return IQ Resulting IQ
	 * 
	 * @throws XmppException If there is any issue while rejecting the call
	 */
	public IQ reject(String callId, RejectCommand reject) throws XmppException {

		return command(reject, callId);
	}
	
	/**
	 * Rejects the call with the id specified as a parameter. 
	 * 
	 * @param callId Id of the call that will be accepted
	 * @return IQ Resulting IQ
	 * 
	 * @throws XmppException If there is any issue while rejecting the call
	 */
	public IQ reject(CallRejectReason reason, String callId) throws XmppException {
		
		RejectCommand reject = new RejectCommand(callId, reason);
		return command(reject, callId);	
	}	

	public VerbRef outputSsml(String ssml, String callId) throws XmppException {
		
		return internalOutput(new Ssml(ssml), callId);
	}

	public VerbRef output(URI uri, String callId) throws XmppException {

		return internalOutput(new Ssml(String.format("<audio src=\"%s\"/>",uri.toString())), callId);
	}	

	public VerbRef output(String text, String callId) throws XmppException {

		return internalOutput(new Ssml(text), callId);
	}
	
	/**
	 * Sends a 'Say' command including some SSML text
	 * 
	 * @param ssml SSML text
	 * @param callId Id of the call to which the say command will be sent 
	 * 
	 * @return VerbRef VerbRef instance that allows to handle the say stream
	 * 
	 * @throws XmppException If there is any issues while sending the say command
	 */
	public VerbRef saySsml(String ssml, String callId) throws XmppException {
		
		return internalSay(new Ssml(ssml), callId);
	}
	
	/**
	 * Sends a 'Say' command to Rayo that will play the specified audio file
	 * 
	 * @param audio URI to the audio file
	 * @param callId Id of the call
	 * 
	 * @return VerbRef VerbRef instance that allows to handle the say stream
	 * 
	 * @throws XmppException If there is any issues while sending the say command
	 * @throws URISyntaxException If the specified audio file is not a valid URI
	 */
	public VerbRef sayAudio(String audio, String callId) throws XmppException, URISyntaxException {
	
		return say(new URI(audio), callId);
	}
	
	/**
	 * Sends a 'Say' command to Rayo that will play the specified audio file
	 * 
	 * @param uri URI to an audio resource that will be played
	 * @param callId Id of the call to which the say command will be sent 
	 * @return VerbRef VerbRef instance that allows to handle the say stream
	 * 
	 * @throws XmppException If there is any issues while sending the say command
	 */
	public VerbRef say(URI uri, String callId) throws XmppException {

		return internalSay(new Ssml(String.format("<audio src=\"%s\"/>",uri.toString())), callId);
	}
	
	/**
	 * Instructs Rayo to say the specified text on the call with the specified id
	 * 
	 * @param text Text that we want to say
	 * @param callId Id of the call to which the say command will be sent 
	 * @return VerbRef VerbRef instance that allows to handle the say stream
	 * 
	 * @throws XmppException If there is any issues while sending the say command
	 */
	public VerbRef say(String text, String callId) throws XmppException {

		return internalSay(new Ssml(text), callId);
	}

	/**
	 * Transfers a specific call to another destination
	 * 
	 * @param to URI where the call will be transfered
	 * @param callId Id of the call we want to transfer
	 * @return IQ Resulting IQ
	 * @throws XmppException If there is any issue while transfering the call
	 */
	public IQ transfer(URI to, String callId) throws XmppException {

		List<URI> list = new ArrayList<URI>();
		list.add(to);
		return transfer(null, list, callId);
	}
	

	/**
	 * Transfers a specific call to another destination
	 * 
	 * @param to URI where the call will be transfered
	 * @param callId Id of the call we want to transfer
	 * @return IQ Resulting IQ
	 * @throws XmppException If there is any issue while transfering the call
	 * @throws URISyntaxException If an invalid URI is passed as a parameter
	 */
	public IQ transfer(String to, String callId) throws XmppException, URISyntaxException {

		return transfer(new URI(to), callId);
	}

	public IQ transfer(List<URI> to, String callId) throws XmppException {

		return transfer(null, to, callId);
	}
	
	/**
	 * Transfers a call to another phone speaking some text before doing the transfer.
	 * 
	 * @param text Text that will be prompted to the user
	 * @param to URI of the call destination
	 * @param callId Id of the call that we want to transfer
	 * @return IQ Resulting IQ
	 * @throws XmppException If there is any issue while transfering the call
	 */
	public IQ transfer(String text, List<URI> to, String callId) throws XmppException {

		Transfer transfer = new Transfer();
		transfer.setTimeout(new Duration(20000));
		transfer.setTerminator('#');

		if (text != null) {
			Ssml ssml = new Ssml(text);
			transfer.setRingbackTone(ssml);
		}
		transfer.setTo(to);
		
		return transfer(transfer, callId);
	}
	
	/**
	 * Transfers a call to another phone with the specified settings
	 * 
	 * @param transfer Model object with all the transfer settings
	 * @param callId Id of the call that we want to transfer

	 * @return IQ Resulting IQ
	 * 
	 * @throws XmppException If there is any issue while transfering the call
	 */
	public IQ transfer(Transfer transfer,String callId) throws XmppException {
		
		IQ iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom())
			.setTo(buildTo(callId))
			.setChild(Extension.create(transfer));
		return sendIQ(iq);
	}
	
	public IQ hold(String callId) throws XmppException {

		HoldCommand hold = new HoldCommand();
		return command(hold,callId);
	}
	
	public IQ unhold(String callId) throws XmppException {

		UnholdCommand unhold = new UnholdCommand();
		return command(unhold,callId);
	}
	
	public IQ mute(String callId) throws XmppException {

		MuteCommand mute = new MuteCommand();
		return command(mute,callId);
	}
	
	public IQ unmute(String callId) throws XmppException {

		UnmuteCommand unmute = new UnmuteCommand();
		return command(unmute,callId);
	}	

	
	/**
	 * Calls a specific destination
	 * 
	 * @param to URI to dial
	 * 
	 * @throws XmppException If there is any issue while dialing
	 */
	public VerbRef dial(URI to) throws XmppException {

		return dial(null, null, to);
	}
	
	
	/**
	 * Sends a dial message to the specified Rayo/Gateway node
	 * to dial a destination from the specified URI
	 * 
	 * @param to URI that we want to dial
	 * 
	 * @throws XmppException If there is any issue while transfering the call
	 */
	public VerbRef dial(String destination, URI to) throws XmppException {

		return dial(destination, null, to);
	}
	
	
	/**
	 * Sends a dial message to the connected node
	 * to dial a destination from the specified URI
	 * 
	 * @param from URI that is dialing
	 * @param to URI that we want to dial
	 * 
	 * @throws XmppException If there is any issue while transfering the call
	 */
	public VerbRef dial(URI from, URI to) throws XmppException {

		return dial(null, from, to);
	}
	
	/**
	 * Sends a dial message to a specific rayo node or gateway 
	 * to dial a destination from the specified URI
	 * 
	 * @param String Rayo/Gateway node
	 * @param from URI that is dialing
	 * @param to URI that we want to dial
	 * 
	 * @throws XmppException If there is any issue while transfering the call
	 */
	public VerbRef dial(String destination, URI from, URI to) throws XmppException {

		DialCommand dial = new DialCommand();
		dial.setTo(to);
		if (from == null) {
			try {
				from = new URI(String.format("sip:%s:5060",InetAddress.getLocalHost().getHostAddress()));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		dial.setFrom(from);
		
		IQ iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom())
			.setTo(rayoServer)
			.setChild(Extension.create(dial));
		
		VerbRef ref = sendAndGetRef(null, iq);
		if (ref == null) {
			throw new DialTimeoutException();
		}
		// dials return a call id on refs, so different than other components
		ref.setCallId(ref.getVerbId());
		return ref;
	}

	private VerbRef sendAndGetRef(String callId, IQ iq) throws XmppException {
		
		Lock lock = connectionLock.readLock();
		lock.lock();
		try {
			VerbRef ref = null;
			IQ result = ((IQ)connection.sendAndWait(iq));
			if (result != null) {
				if (result.hasChild("error")) {
					throw new XmppException(result.getError());
				}
				RefEvent reference = (RefEvent)result.getExtension().getObject();
				ref = new VerbRef(callId, reference.getJid());
				return ref;
			} else {
				return null;
			}
		} finally {
			lock.unlock();
		}		
	}
	
	/**
	 * Instructs Rayo to ask a question with the specified choices on the call with the given id  
	 * 
	 * @param text Text that will be prompted
	 * @param choicesText Choices
	 * @param callId Id of the call in which the question will be asked
	 * @return IQ Resulting IQ
	 * @throws XmppException If there is any issue while asking the question 
	 */
	public IQ ask(String text, String choicesText, String callId) throws XmppException {
		
		Ask ask = new Ask();

		Ssml ssml = new Ssml(text);
		ask.setPrompt(ssml);

		List<Choices> list = new ArrayList<Choices>();
		Choices choices = new Choices();
		choices.setContent(choicesText);
		choices.setContentType("application/grammar+voxeo");
		list.add(choices);
		ask.setChoices(list);
		ask.setTerminator('#');
		ask.setMode(InputMode.DTMF);
		ask.setTimeout(new Duration(650000));
		
		IQ iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom())
			.setTo(buildTo(callId))
			.setChild(Extension.create(ask));
		return sendIQ(iq);
	}
	
	
	public VerbRef input(String simpleGrammar, String callId) throws XmppException {
		
		Input input = new Input();
		List<Choices> choices = new ArrayList<Choices>();
		Choices choice = new Choices();
		choice.setContent(simpleGrammar);
		choice.setContentType("application/grammar+voxeo");
		choices.add(choice);
		input.setGrammars(choices);
		
		return input(input, callId);
	}
	
	public VerbRef input(Input input, String callId) throws XmppException {
		
		IQ iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom())
			.setTo(buildTo(callId))
			.setChild(Extension.create(input));
		
		return sendAndGetRef(callId, iq);
	}
	
	/**
	 * Creates a conference and joins the last active call 
	 * 
	 * @param roomName Id of the conference
	 * @param callId Id of the call that will be starting the conference
	 * 
	 * @return VerbRef A reference to the conference object that has been created
	 * 
	 * @throws XmppException If there is any problem while creating the conference
	 */
	public VerbRef conference(String roomName, String callId) throws XmppException {
		
		Conference conference = new Conference();
		conference.setRoomName(roomName);
		conference.setTerminator('#');
		return conference(conference,callId);
	}
	
	/**
	 * Creates a conference and joins the last active call 
	 * 
	 * @param conference Conference object
	 * @param callId Id of the call that will be starting the conference
	 * 
	 * @return VerbRef A reference to the conference object that has been created
	 * 
	 * @throws XmppException If there is any problem while creating the conference
	 */
	public VerbRef conference(Conference conference, String callId) throws XmppException {
		
		IQ iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom())
			.setTo(buildTo(callId))
			.setChild(Extension.create(conference));
		
		return sendAndGetRef(callId, iq);
	}
	
	private VerbRef internalSay(Ssml item, String callId) throws XmppException {
		
		Say say = new Say();
		say.setPrompt(item);
		
		IQ iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom())
			.setTo(buildTo(callId))
			.setChild(Extension.create(say));
		
		return sendAndGetRef(callId, iq);
	}
	
	private VerbRef internalOutput(Ssml item, String callId) throws XmppException {
		
		Output output = new Output();
		output.setPrompt(item);
		
		return output(output, callId);
	}
	
	public VerbRef output(Output output, String callId) throws XmppException {
		
		IQ iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom())
			.setTo(buildTo(callId))
			.setChild(Extension.create(output));
		
		return sendAndGetRef(callId, iq);
	}
	
	/**
	 * Pauses a verb component
	 * 
	 * @param ref Verb component that we want to pause
	 * @return IQ Resulting IQ
	 */
	public IQ pause(VerbRef ref) throws XmppException {
		
		ClientPauseCommand pause = new ClientPauseCommand();
		IQ iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom())
			.setTo(buildTo(ref.getCallId(),ref.getVerbId()))
			.setChild(Extension.create(pause));
		return sendIQ(iq);
	}
	
	/**
	 * Resumes a verb component
	 * 
	 * @param ref Verb component that we want to resume
	 * @return IQ Resulting IQ
	 */
	public IQ resume(VerbRef ref) throws XmppException {
		
		ClientResumeCommand resume = new ClientResumeCommand();
		IQ iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom())
			.setTo(buildTo(ref.getCallId(),ref.getVerbId()))
			.setChild(Extension.create(resume));
		return sendIQ(iq);
	}
	
	/**
	 * Speeds up
	 * 
	 * @param ref Verb component that we want to speed up
	 * @return IQ Resulting IQ
	 */
	public IQ speedUp(VerbRef ref) throws XmppException {
		
		SpeedUpCommand speedup = new SpeedUpCommand();
		IQ iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom())
			.setTo(buildTo(ref.getCallId(),ref.getVerbId()))
			.setChild(Extension.create(speedup));
		return sendIQ(iq);
	}
	
	/**
	 * Speeds down
	 * 
	 * @param ref Verb component that we want to speed up
	 * @return IQ Resulting IQ
	 */
	public IQ speedDown(VerbRef ref) throws XmppException {
		
		SpeedDownCommand speedDown = new SpeedDownCommand();
		IQ iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom())
			.setTo(buildTo(ref.getCallId(),ref.getVerbId()))
			.setChild(Extension.create(speedDown));
		return sendIQ(iq);
	}
	
	
	/**
	 * Turn volume up
	 * 
	 * @param ref Verb component that we want to turn volume up
	 * @return IQ Resulting IQ
	 */
	public IQ volumeUp(VerbRef ref) throws XmppException {
		
		VolumeUpCommand volumeUp = new VolumeUpCommand();
		IQ iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom())
			.setTo(buildTo(ref.getCallId(),ref.getVerbId()))
			.setChild(Extension.create(volumeUp));
		return sendIQ(iq);
	}
	
	/**
	 * Turn volume down
	 * 
	 * @param ref Verb component that we want to turn volume down
	 * @return IQ Resulting IQ
	 */
	public IQ volumeDown(VerbRef ref) throws XmppException {
		
		VolumeDownCommand volumeDown = new VolumeDownCommand();
		IQ iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom())
			.setTo(buildTo(ref.getCallId(),ref.getVerbId()))
			.setChild(Extension.create(volumeDown));
		return sendIQ(iq);
	}
	
	/**
	 * Pauses a records component
	 * 
	 * @param ref Verb component that we want to pause
	 */
	public IQ pauseRecord(VerbRef ref) throws XmppException {
		
		RecordPauseCommand pause = new RecordPauseCommand();
		IQ iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom())
			.setTo(buildTo(ref.getCallId(),ref.getVerbId()))
			.setChild(Extension.create(pause));
		return sendIQ(iq);
	}
	
	/**
	 * Resumes a record component
	 * 
	 * @param ref Verb component that we want to resume
	 */
	public IQ resumeRecord(VerbRef ref) throws XmppException {
		
		RecordResumeCommand resume = new RecordResumeCommand();
		IQ iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom())
			.setTo(buildTo(ref.getCallId(),ref.getVerbId()))
			.setChild(Extension.create(resume));
		return sendIQ(iq);
	}
	
	/**
	 * Performs a seek operation on the given verb
	 * 
	 * @param ref Verb component that we want to resume
	 * @param command Seek command to execute
	 */
	public IQ seek(VerbRef ref, SeekCommand command) throws XmppException {
		
		IQ iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom())
			.setTo(buildTo(ref.getCallId(),ref.getVerbId()))
			.setChild(Extension.create(command));
		return sendIQ(iq);
	}
	
	/**
	 * Stops a verb component
	 * 
	 * @param ref Verb component that we want to stop
	 */
	public IQ stop(VerbRef ref) throws XmppException {
		
		StopCommand stop = new StopCommand();
		IQ iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom())
			.setTo(buildTo(ref.getCallId(),ref.getVerbId()))
			.setChild(Extension.create(stop));
		return sendIQ(iq);
	}
	
	public VerbRef record(String callId) throws XmppException {
		
		return record(new Record(), callId);
	}
	
	public VerbRef record(Record record, String callId) throws XmppException {
		
		IQ iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom())
			.setTo(buildTo(callId))
			.setChild(Extension.create(record));
		
		return sendAndGetRef(callId, iq);
	}
	
	/**
	 * Hangs up the specified call id
	 * 
	 * @param callId Id of the call to be hung up
	 * @return IQ Resulting IQ
	 */
	public IQ hangup(String callId) throws XmppException {
		
		return hangup(callId, new HangupCommand(null));
	}
	
	
	/**
	 * Hangs up the specified call id
	 * 
	 * @param command Hangup command
	 * @return IQ Resulting IQ
	 */
	public IQ hangup(String callId, HangupCommand command) throws XmppException {
		
		IQ iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom())
			.setTo(buildTo(callId))
			.setChild(Extension.create(command));
		return sendIQ(iq);
	}

	public IQ unjoin(String from, JoinDestinationType type, String callId) throws XmppException {
		
		UnjoinCommand unjoin = new UnjoinCommand();
		unjoin.setFrom(from);
		unjoin.setType(type);
		
		return command(unjoin,callId);
	}

	public IQ join(String to, String media, String direction, JoinDestinationType type, String callId) throws XmppException {
		
		JoinCommand join = new JoinCommand();
		join.setTo(to);
		join.setDirection(Joinable.Direction.DUPLEX);
		join.setMedia(JoinType.BRIDGE);
		join.setType(type);
		
		return command(join,callId);
	}
	
	public IQ join(JoinCommand join, String callId) throws XmppException {
		
		return command(join,callId);
	}
	
	public IQ dtmf(String tones, String callId) throws XmppException {
		
		DtmfCommand dtmf = new DtmfCommand(tones);
		return command(dtmf, callId);
	}
	
	public IQ command(CallCommand command, String callId) throws XmppException {
        IQ iq = new IQ(IQ.Type.set)
            .setFrom(buildFrom())
            .setTo(buildTo(callId))
            .setChild(Extension.create(command));
        return sendIQ(iq);
	}
	
	public VerbRef dial(DialCommand command) throws XmppException {
        
		IQ iq = new IQ(IQ.Type.set)
            .setFrom(buildFrom())
            .setTo(rayoServer) 
            .setChild(Extension.create(command));
        VerbRef ref = sendAndGetRef(null, iq);
        
        if (ref == null) {
        	throw new DialTimeoutException();
        }
		// dials return a call id on refs, so different than other components
		ref.setCallId(ref.getVerbId());
        return ref;
	}
	
	/**
	 * Redirects an existing call to the given URI
	 * 
	 * @param uri URI for redirecting the call to
	 * @param callId Id of the call to redirect
	 */
	public IQ redirect(URI uri, String callId) throws XmppException {
		
		RedirectCommand redirect = new RedirectCommand();
		redirect.setTo(uri);
		return redirect(redirect, callId);
	}
	
	
	/**
	 * Redirects an existing call
	 * 
	 * @param command Redirect command
	 * @param callId Id of the call to redirect
	 * @return IQ Resulting IQ
	 */
	public IQ redirect(RedirectCommand command, String callId) throws XmppException {
		
		return command(command, callId);
	}
	
	protected IQ sendIQ(IQ iq) throws XmppException {
		
		Lock lock = connectionLock.readLock();
		lock.lock();
		try {
			return (IQ)connection.sendAndWait(iq);
		} finally {
			lock.unlock();
		}			
	}
	
	private String buildFrom() {
		
		return connection.getUsername() + "@" + connection.getServiceName() + "/" + connection.getResource();
	}
	
	private String buildTo(String callId) {
		
		return buildTo(callId, null);
	}

	private String buildTo(String callId, String resourceId) {
		
		String to = callId + "@" + rayoServer;
		if (resourceId != null) {
			to = to + "/" + resourceId;
		}
		return to;
	}
	
	public XmppConnection getXmppConnection() {
		
		return connection;
	}

	private void ping() {
		
		Lock lock = connectionLock.readLock();
		lock.lock();
		try {
			if (connection.isConnected()) {
				IQ ping = new IQ(IQ.Type.get)
					.setFrom(buildFrom())
					.setTo(rayoServer)
					.setChild(new Ping());
				try {
					connection.send(ping);
				} catch (XmppException e) {
					e.printStackTrace();
				}
			}
		} finally {
			lock.unlock();
		}		
	}
}

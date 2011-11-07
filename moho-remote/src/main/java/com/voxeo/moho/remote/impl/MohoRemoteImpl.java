package com.voxeo.moho.remote.impl;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.rayo.client.RayoClient;
import com.rayo.client.XmppException;
import com.rayo.client.listener.StanzaListener;
import com.rayo.client.xmpp.stanza.IQ;
import com.rayo.client.xmpp.stanza.Message;
import com.rayo.client.xmpp.stanza.Presence;
import com.rayo.core.OfferEvent;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.Participant;
import com.voxeo.moho.common.event.DispatchableEventSource;
import com.voxeo.moho.common.util.Utils.DaemonThreadFactory;
import com.voxeo.moho.remote.AuthenticationCallback;
import com.voxeo.moho.remote.MohoRemote;

public class MohoRemoteImpl extends DispatchableEventSource implements MohoRemote {

  protected static final Logger LOG = Logger.getLogger(MohoRemoteImpl.class);

  protected RayoClient _client;

  protected ThreadPoolExecutor _executor;

  protected Map<String, ParticipantImpl> _participants = new ConcurrentHashMap<String, ParticipantImpl>();

  protected Lock _participanstLock = new ReentrantLock();

  public MohoRemoteImpl() {
    super();
    // TODO make configurable
    int eventDispatcherThreadPoolSize = 10;
    _executor = new ThreadPoolExecutor(eventDispatcherThreadPoolSize, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
        new SynchronousQueue<Runnable>(), new DaemonThreadFactory("MohoContext"));
    _dispatcher.setExecutor(_executor, false);
  }

  // TODO ask the thread model of RayoClient.
  class MohoStanzaListener implements StanzaListener {

    @Override
    public void onIQ(IQ iq) {
      // dispatch the stanza to corresponding call.
      JID fromJID = new JID(iq.getFrom());
      String id = fromJID.getNode();
      if (id != null) {
        ParticipantImpl participant = MohoRemoteImpl.this.getParticipant(id);
        if (participant != null) {
          participant.onRayoCommandResult(fromJID, iq);
        }
        else {
          MohoRemoteImpl.this.LOG.error("Can't find participant for rayo event:" + iq);
        }
      }
    }

    @Override
    public void onMessage(Message message) {
      MohoRemoteImpl.this.LOG.error("Received message from rayo:" + message);
    }

    @Override
    public void onPresence(Presence presence) {
      JID fromJID = new JID(presence.getFrom());
      if (!presence.hasExtension()) {
        return;
      }
      if (presence.getExtension().getStanzaName().equalsIgnoreCase("offer")) {
        OfferEvent offerEvent = (OfferEvent) presence.getExtension().getObject();

        IncomingCallImpl call = new IncomingCallImpl(MohoRemoteImpl.this, fromJID.getNode(),
            createEndpoint(offerEvent.getFrom()), createEndpoint(offerEvent.getTo()), offerEvent.getHeaders());

        MohoRemoteImpl.this.dispatch(call);
      }
      else {
        // dispatch the stanza to corresponding call.
        String callID = fromJID.getNode();
        ParticipantImpl participant = MohoRemoteImpl.this.getParticipant(callID);
        if (participant != null) {
          participant.onRayoEvent(fromJID, presence);
        }
        else {
          MohoRemoteImpl.this.LOG.error("Can't find call for rayo event:" + presence);
        }
      }
    }

    @Override
    public void onError(com.rayo.client.xmpp.stanza.Error error) {
      MohoRemoteImpl.this.LOG.error("Got error stanza:" + error);
    }
  }

  @Override
  public ParticipantImpl getParticipant(final String cid) {
    getParticipantsLock().lock();
    try {
      return _participants.get(cid);
    }
    finally {
      getParticipantsLock().unlock();
    }
  }

  protected void addParticipant(final ParticipantImpl participant) {
    _participants.put(participant.getId(), participant);
  }

  protected void removeParticipant(final String id) {
    getParticipantsLock().lock();
    try {
      _participants.remove(id);
    }
    finally {
      getParticipantsLock().unlock();
    }
  }

  public Lock getParticipantsLock() {
    return _participanstLock;
  }

  // TODO connection error handling, ask 
  @Override
  public void connect(AuthenticationCallback callback, String xmppServer, String rayoServer) {
    connect(callback.getUserName(), callback.getPassword(), callback.getRealm(), callback.getResource(), xmppServer,
        rayoServer);
  }

  @Override
  public synchronized void connect(String userName, String passwd, String realm, String resource, String xmppServer,
      String rayoServer) {
    if (_client == null) {
      _client = new RayoClient(xmppServer, rayoServer);
      _client.addStanzaListener(new MohoStanzaListener());
    }
    try {
      _client.connect(userName, passwd, resource);
    }
    catch (XmppException e) {
      LOG.error("Error connecting server", e);
    }
  }

  @Override
  public synchronized void disconnect() {
    Collection<ParticipantImpl> participants = _participants.values();
    for (Participant participant : participants) {
      participant.disconnect();
    }

    try {
      _client.disconnect();
    }
    catch (XmppException e) {
      LOG.error("", e);
    }

    _executor.shutdown();
  }

  @Override
  public CallableEndpoint createEndpoint(URI uri) {
    return new CallableEndpointImpl(this, uri);
  }

  public Executor getExecutor() {
    return _executor;
  }

  public RayoClient getRayoClient() {
    return _client;
  }
}
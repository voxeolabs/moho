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
import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.Participant;
import com.voxeo.moho.common.event.DispatchableEventSource;
import com.voxeo.moho.remote.AuthenticationCallback;
import com.voxeo.moho.remote.MohoRemote;
import com.voxeo.moho.remote.MohoRemoteException;
import com.voxeo.moho.remote.impl.utils.Utils.DaemonThreadFactory;

@SuppressWarnings("deprecation")
public class MohoRemoteImpl extends DispatchableEventSource implements MohoRemote {

  protected static final Logger LOG = Logger.getLogger(MohoRemoteImpl.class);

  protected RayoClient _client;

  protected ThreadPoolExecutor _executor;

  protected Map<String, Participant> _participants = new ConcurrentHashMap<String, Participant>();

  protected Lock _componentCommandLock = new ReentrantLock();

  public MohoRemoteImpl() {
    super();
    int eventDispatcherThreadPoolSize = 10;
    _executor = new ThreadPoolExecutor(eventDispatcherThreadPoolSize, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
        new SynchronousQueue<Runnable>(), new DaemonThreadFactory("MohoContext"));
    _dispatcher.setExecutor(_executor, false);
  }

@Override
  public void connect(AuthenticationCallback callback, String xmppServer, String rayoServer) throws MohoRemoteException {
    connect(callback.getUserName(), callback.getPassword(), callback.getRealm(), callback.getResource(), xmppServer,
        rayoServer);
  }

  @Override
  public void disconnect() throws MohoRemoteException {
    Collection<Participant> participants = _participants.values();
    for (Participant participant : participants) {
      participant.disconnect();
    }

    try {
      _client.disconnect();
    }
    catch (XmppException e) {
     	throw new MohoRemoteException(e);
    }

    _executor.shutdown();
  }

  class MohoStanzaListener implements StanzaListener {

    @Override
    public void onIQ(IQ iq) {
      // dispatch the stanza to corresponding call.
      JID fromJID = new JID(iq.getFrom());
      String id = fromJID.getNode();
      if (id != null) {
        Participant participant = MohoRemoteImpl.this.getParticipant(id);
        if (participant != null) {
          // TODO crate a parent class to implement the RayoListener
          if (participant instanceof Call) {
            CallImpl call = (CallImpl) participant;
            call.onRayoCommandResult(fromJID, iq);
          }

        }
        else {
          LOG.error("Can't find call for rayo event:" + iq);
        }
      }
    }

    @Override
    public void onMessage(Message message) {
      LOG.error("Received message from rayo:" + message);
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
        Participant participant = MohoRemoteImpl.this.getParticipant(callID);
        if (participant != null) {
          // TODO crate a parent class to implement the RayoListener
          if (participant instanceof Call) {
            CallImpl call = (CallImpl) participant;
            call.onRayoEvent(fromJID, presence);
          }
        }
        else {
          LOG.error("Can't find call for rayo event:" + presence);
        }
      }
    }

    @Override
    public void onError(com.rayo.client.xmpp.stanza.Error error) {
      LOG.error("Got error" + error);

    }
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

  @Override
  public Participant getParticipant(final String cid) {
    _componentCommandLock.lock();
    Participant participant = null;
    try {
      participant = _participants.get(cid);
    }
    finally {
      _componentCommandLock.unlock();
    }
    return participant;
  }

  protected void addCall(final CallImpl call) {
    _participants.put(call.getId(), call);
  }

  protected void removeCall(final String id) {
    _participants.remove(id);
  }

  @Override
  public void connect(String userName, String passwd, String realm, String resource, String xmppServer,
      String rayoServer) throws MohoRemoteException {
    if (_client == null) {
      _client = new RayoClient(xmppServer, rayoServer);

      try {
        _client.connect(userName, passwd, resource);
      }
      catch (XmppException e) {
       	throw new MohoRemoteException(e);
      }

      _client.addStanzaListener(new MohoStanzaListener());
    }
  }

  public Lock getComponentCommandLock() {
    return _componentCommandLock;
  }

}

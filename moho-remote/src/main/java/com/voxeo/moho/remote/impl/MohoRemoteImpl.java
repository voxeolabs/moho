package com.voxeo.moho.remote.impl;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
import com.voxeo.moho.remote.AuthenticationCallback;
import com.voxeo.moho.remote.MohoRemote;
import com.voxeo.moho.remote.impl.event.DispatchableEventSource;
import com.voxeo.moho.remote.impl.utils.Utils.DaemonThreadFactory;

public class MohoRemoteImpl extends DispatchableEventSource implements MohoRemote {

  protected static final Logger LOG = Logger.getLogger(MohoRemoteImpl.class);

  protected RayoClient _client;

  protected ThreadPoolExecutor _executor;

  protected Map<String, CallImpl> _calls = new ConcurrentHashMap<String, CallImpl>();

  public MohoRemoteImpl() {
    super();
    int eventDispatcherThreadPoolSize = 10;
    _executor = new ThreadPoolExecutor(eventDispatcherThreadPoolSize, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
        new SynchronousQueue<Runnable>(), new DaemonThreadFactory("MohoContext"));
    _dispatcher.setExecutor(_executor, false);
  }

  @Override
  public void connect(AuthenticationCallback callback, String server) {
    _client = new RayoClient(server);

    try {
      _client.connect(callback.getUserName(), callback.getPassword(), callback.getResource());
    }
    catch (XmppException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    _client.addStanzaListener(new MohoStanzaListener());
  }

  @Override
  public void disconnect() {
    _executor.shutdown();

    try {
      _client.disconnect();
    }
    catch (XmppException e) {
      LOG.error("", e);
    }

    Collection<CallImpl> calls = _calls.values();
    for (Call call : calls) {
      call.disconnect();
    }
  }

  class MohoStanzaListener implements StanzaListener {

    @Override
    public void onIQ(IQ iq) {
      // dispatch the stanza to corresponding call.
      JID fromJID = new JID(iq.getFrom());
      String callID = fromJID.getNode();
      if (callID != null) {
        CallImpl call = MohoRemoteImpl.this._calls.get(callID);
        if (call != null) {
          call.onRayoCommandResult(fromJID, iq);
        }
        else {
          MohoRemoteImpl.this.LOG.error("Can't find call for rayo event:" + iq);
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
      if (presence.getExtension().getStanzaName().equalsIgnoreCase("offer")) {
        OfferEvent offerEvent = (OfferEvent) presence.getExtension().getObject();

        IncomingCallImpl call = new IncomingCallImpl(MohoRemoteImpl.this, fromJID.getNode(),
            createEndpoint(offerEvent.getFrom()), createEndpoint(offerEvent.getTo()), offerEvent.getHeaders());

        MohoRemoteImpl.this.dispatch(call);
      }
      else {
        // dispatch the stanza to corresponding call.
        String callID = fromJID.getNode();
        CallImpl call = MohoRemoteImpl.this._calls.get(callID);
        if (call != null) {
          call.onRayoEvent(fromJID, presence);
        }
        else {
          MohoRemoteImpl.this.LOG.error("Can't find call for rayo event:" + presence);
        }
      }
    }

    @Override
    public void onError(com.rayo.client.xmpp.stanza.Error error) {
      MohoRemoteImpl.this.LOG.error("Got error" + error);

    }
  }

  @Override
  public CallableEndpoint createEndpoint(URI uri) {
    return new CallableEndpointImpl(this, uri);
  }

  protected Executor getExecutor() {
    return _executor;
  }

  public RayoClient getRayoClient() {
    return _client;
  }

  @Override
  public Call getCall(final String cid) {
    return _calls.get(cid);
  }

  protected void addCall(final CallImpl call) {
    _calls.put(call.getId(), call);
  }

  protected void removeCall(final String id) {
    _calls.remove(id);
  }
}

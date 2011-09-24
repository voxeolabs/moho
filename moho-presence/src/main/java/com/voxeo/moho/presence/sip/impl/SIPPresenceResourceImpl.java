package com.voxeo.moho.presence.sip.impl;

import java.util.List;

import com.voxeo.moho.event.SubscribeEvent.SubscriptionContext;
import com.voxeo.moho.presence.NotifyBody;
import com.voxeo.moho.presence.SubscriptionID;
import com.voxeo.moho.presence.impl.StoreHolder;
import com.voxeo.moho.presence.impl.sip.SIPPresenceStore;
import com.voxeo.moho.presence.sip.EventSoftState;
import com.voxeo.moho.presence.sip.SIPPresenceResource;
import com.voxeo.moho.presence.sip.SIPPresenceService;
import com.voxeo.moho.presence.sip.SipSubscriptionState;
import com.voxeo.moho.presence.sip.impl.notifybody.PIDFNotifyBody;
import com.voxeo.moho.sip.SIPSubscribeEvent.SIPSubscriptionContext;
import com.voxeo.moho.spi.ExecutionContext;

public class SIPPresenceResourceImpl extends AbstractSIPResource implements SIPPresenceResource {

  private static final long serialVersionUID = 4701464202988858838L;

  public SIPPresenceResourceImpl(ExecutionContext context, String resourceUri, String eventName) {
    super(context, resourceUri, eventName);
  }

  @Override
  public SipSubscriptionState addSubscription(SubscriptionContext context) {
    insertSubscriptionContext(context);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Added " + context + "on " + this);
    }
    SIPSubscriptionContextImpl impl = (SIPSubscriptionContextImpl) context;
    impl.setState(SipSubscriptionStateImpl.ALLOW);
    return SipSubscriptionStateImpl.ALLOW;
  }

  @Override
  public SipSubscriptionState updateSubscripton(SubscriptionContext context) {
    insertSubscriptionContext(context);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Updated " + context + "on " + this);
    }
    return SipSubscriptionStateImpl.ALLOW;
  }

  @Override
  public SipSubscriptionState removeSubscripton(SubscriptionContext context) {
    removeSubscriptionContext(context);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Removed " + context + "on " + this);
    }
    SIPSubscriptionContextImpl impl = (SIPSubscriptionContextImpl) context;
    impl.setState(SipSubscriptionStateImpl.TERMINATED);
    return SipSubscriptionStateImpl.TERMINATED;
  }

  @Override
  public void addEventSoftState(EventSoftState softState) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Add " + softState + " for " + this);
    }
    insertEventSoftState(softState);
  }
  
  @Override
  public void refreshEventSoftState(EventSoftState softState) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Refresh " + softState + " for " + this);
    }
    insertEventSoftState(softState);
  }

  @Override
  public EventSoftState getSoftState(String sipIfMatch) {
    SIPPresenceStore presenceStore = (SIPPresenceStore) StoreHolder.getPresenceStore();
    return presenceStore.getEventSoftState(getUri(), sipIfMatch);
  }

  @Override
  public void updateEventSoftState(EventSoftState softState) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Update " + softState + " for " + this);
    }
    insertEventSoftState(softState);
  }
  
  private void insertEventSoftState(EventSoftState softState) {
    SIPPresenceStore presenceStore = (SIPPresenceStore) StoreHolder.getPresenceStore();
    presenceStore.addEventSoftState(softState);
    setNotifyBody(presenceStore, softState.getContentType(), softState.getBody());
  }
  
  private void setNotifyBody(SIPPresenceStore presenceStore, String notifyBodyName, NotifyBody notifyBody) {
    presenceStore.addNotifyBody(getUri(), getEventName(), notifyBodyName, notifyBody);
    List<SubscriptionID> subscriptions = presenceStore.getSubscriptions(getUri(), getEventName(), notifyBodyName);
    if (subscriptions != null) {
      for (SubscriptionID id : subscriptions) {
        SIPSubscriptionContext subscription = (SIPSubscriptionContext) presenceStore.getSubscription(id);
        if (subscription == null) {
          LOG.warn("Can't find subscription on " + id + " for " + this);
        }
        try {
          sendNotify(subscription);
        }
        catch (Exception e) {
          LOG.error("Can't send notify on " + subscription, e);
        }
      }
    }
  }

  public void sendNotify(SIPSubscriptionContext subscription) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Will send notify to found " + subscription + " for " + this + " due to status change");
    }
    Runnable sendNotify = subscription.sendNotify();
    SIPPresenceService service = _context.getService(SIPPresenceService.class);
    try {
      service.getNotifyDispatcher().put((NotifyRequest) sendNotify);
    }
    catch (InterruptedException e) {
      ;
    }
  }

  @Override
  public void removeEventSoftState(EventSoftState softState) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Remove " + softState + " for " + this);
    }
    SIPPresenceStore presenceStore = (SIPPresenceStore) StoreHolder.getPresenceStore();
    presenceStore.removeEventSoftState(softState);
    setNotifyBody(presenceStore, softState.getContentType(), createNeutralBody());
  }

  @Override
  protected NotifyBody createNeutralBody() {
    StringBuilder result = new StringBuilder();
    result.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    result.append("<presence xmlns=\"urn:ietf:params:xml:ns:pidf\" entity=\"").append(getUri()).append("\">\n");
    result.append("  <tuple id=\"neutral_").append(Utils.generate() + "\">\n");
    result.append("    <status><basic>closed</basic></status>\n");
    result.append("  </tuple>\n");
    result.append("</presence>");
    return new PIDFNotifyBody(result.toString());
  }

  @Override
  public String toString() {
    return "SIPPresenceResource [EventName=" + getEventName() + ", Uri=" + getUri() + "]";
  }
  
  
}

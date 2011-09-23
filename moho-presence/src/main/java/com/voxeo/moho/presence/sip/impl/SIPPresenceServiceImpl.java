package com.voxeo.moho.presence.sip.impl;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.sip.SipServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.voxeo.moho.event.AcceptableEvent;
import com.voxeo.moho.event.AcceptableEvent.Reason;
import com.voxeo.moho.event.SubscribeEvent.SubscriptionContext;
import com.voxeo.moho.presence.NotifyBody;
import com.voxeo.moho.presence.NotifyDispatcher;
import com.voxeo.moho.presence.Resource;
import com.voxeo.moho.presence.StoreRetrieveListener;
import com.voxeo.moho.presence.SubscriptionState;
import com.voxeo.moho.presence.impl.StoreHolder;
import com.voxeo.moho.presence.impl.sip.SIPPresenceStore;
import com.voxeo.moho.presence.impl.sip.memory.SIPMemoryPresenceStore;
import com.voxeo.moho.presence.sip.EventSoftState;
import com.voxeo.moho.presence.sip.SIPPresenceResource;
import com.voxeo.moho.presence.sip.SIPPresenceService;
import com.voxeo.moho.presence.sip.SIPResource;
import com.voxeo.moho.presence.sip.SipPresenceFactory;
import com.voxeo.moho.sip.SIPPublishEvent;
import com.voxeo.moho.sip.SIPSubscribeEvent;
import com.voxeo.moho.sip.SIPSubscribeEvent.SIPSubscriptionContext;
import com.voxeo.moho.spi.ExecutionContext;

public class SIPPresenceServiceImpl implements SIPPresenceService {
  
  private final static Logger LOG = Logger.getLogger(SIPPresenceServiceImpl.class);
  
  private ExecutionContext _context;
  
  private SIPPresenceStore _store;
  
  private SipPresenceFactory _presenceFactory;
  
  private NotifyDispatcher _notifyDispatcher;
  
  @Override
  public void init(ExecutionContext context, Map<String, String> props) {
    _context = context;
    _presenceFactory = _context.getService(SipPresenceFactory.class);
    String storeImpl = props.get(STORE_IMPL);
    if (storeImpl == null) {
      storeImpl = SIPMemoryPresenceStore.class.getName();
    }
    try {
      _store = (SIPPresenceStore) Class.forName(storeImpl).newInstance();
      _store.init(props);
      _store.addRetrieveListener(Resource.class, new StoreRetrieveListener<Resource>() {
        @Override
        public void onRetrieve(Resource resource) {
          resource.setExecutionContext(_context);
        }
      });
      _store.addRetrieveListener(SubscriptionContext.class, new StoreRetrieveListener<SubscriptionContext>() {
        @Override
        public void onRetrieve(SubscriptionContext resource) {
          resource.setExecutionContext(_context);
        }
      });
    }
    catch (Exception e) {
      throw new IllegalArgumentException("Invalidate Presence Store implementation: " + e);
    }
    int capacity = 1000;
    _notifyDispatcher = new MemoryNotifyDispatcher(context.getExecutor(), capacity);
    context.getExecutor().execute(_notifyDispatcher);
    context.getExecutor().execute(this);
  }

  @Override
  public void doPublish(SIPPublishEvent event) {
    try {
      SipServletRequest req = event.getSipRequest();
      //validate expire
      int expires = req.getExpires();
      if (_presenceFactory.checkPublishExpires(expires) < 0) {
        rejectMinExpire(event, _presenceFactory);
      }
      byte[] content = req.getRawContent();
      // for especially return byte[0]
      if (content != null && content.length == 0) {
        content = null;
      }
      StoreHolder.setPresenceStore(_store);
      //validate supported notifyBodyType
      //getResource
      final String resourceKey = Utils.getCleanUri(req.getTo().getURI()).toString();
      EventHeader eventHeader = new EventHeader(req.getHeader("Event"));
      SIPPresenceResource resource = (SIPPresenceResource) getResource(_presenceFactory, resourceKey, eventHeader.getEventName());
      String contentType = req.getContentType();
      if (StringUtils.isEmpty(contentType)) {
        contentType = _presenceFactory.getDefaultNotifyBodyName(eventHeader.getEventName());
      }
      final String sipIfMatch = event.getSipRequest().getHeader("SIP-If-Match");
      EventSoftState eventSoftState = null;
      // This is the first publish request
      if (content != null && sipIfMatch == null && expires > 0) {
        NotifyBody notifyBody = _presenceFactory.createNotifyBody(event.getSipRequest());
        eventSoftState = new SIPEventSoftState(resourceKey, eventHeader.getEventName(), expires, contentType, notifyBody);
        resource.addEventSoftState(eventSoftState);
        acceptPublish(event, eventSoftState);
      }
      // Publish keepalive
      else if(content == null && sipIfMatch != null && expires > 0) {
        eventSoftState = resource.getSoftState(sipIfMatch);
        if (eventSoftState != null) {
          eventSoftState.updateExpires(expires);
          resource.refreshEventSoftState(eventSoftState);
          acceptPublish(event, eventSoftState);
        }
        else {
          event.reject(Reason.CONDITIONAL_REQUEST_FAILED);
        }
      }
      // publish modify
      else if (content != null && sipIfMatch != null && expires > 0) {
        NotifyBody notifyBody = _presenceFactory.createNotifyBody(event.getSipRequest());
        eventSoftState = resource.getSoftState(sipIfMatch);
        if (eventSoftState != null) {
          eventSoftState.setBody(notifyBody);
          eventSoftState.updateExpires(expires);
          resource.refreshEventSoftState(eventSoftState);
          acceptPublish(event, eventSoftState);
        }
        else {
          event.reject(Reason.CONDITIONAL_REQUEST_FAILED);
        }
      }
      // publish removal
      else if (content == null && sipIfMatch != null && expires == 0) {
        eventSoftState = resource.getSoftState(sipIfMatch);
        if (eventSoftState != null) {
          resource.removeEventSoftState(eventSoftState);
          event.accept();
        }
        else {
          event.reject(Reason.CONDITIONAL_REQUEST_FAILED);
        }
      }
      // bad request
      else {
        LOG.warn("Bad publish request[SIP-If-Match=" + sipIfMatch + ", expires=" + expires + ", hasContent=" + content != null);
        event.reject(Reason.BAD_REQUEST);
      }
    }
    catch (Exception e) {
      LOG.error("Error handling subscribe", e);
      event.reject(Reason.ERROR);
    }
    finally {
      StoreHolder.setPresenceStore(null);
    }
  }

  @Override
  public void doSubscribe(SIPSubscribeEvent event) {
    if (isResponsibleFor(event)) {
      try {
        SIPSubscriptionContext subscription = (SIPSubscriptionContext) event.getSubscription();
        //TODO support resourcelist
        //verify event
        if (subscription.getEventName() == null || !SIPConstans.EVENT_NAME_PRESENCE.equals(subscription.getEventName())) {
          HashMap<String, String> headers = new HashMap<String, String>();
          headers.put("Allow-Events", SIPConstans.EVENT_NAME_PRESENCE);
          event.reject(Reason.BAD_EVENT, headers);
          return;
        }
        //verify expires
        int expire = _presenceFactory.checkSubscribeExpires(subscription.getExpires());
        if (expire < 0) {
          rejectMinExpire(event, _presenceFactory);
        }
        
        _store.startTx();
        StoreHolder.setPresenceStore(_store);
        //get resource
        SIPResource resource = getResource(_presenceFactory, subscription.getSubscribee(), subscription.getEventName());
        
        SubscriptionState state = null;
        //update subscription context
        if (_store.isSubscriptionExist(subscription)) {
          if (subscription.getExpires() == 0) {
            state = resource.removeSubscripton(subscription);
          }
          else {
            state = resource.updateSubscripton(subscription);
          }
        }
        else {
          state = resource.addSubscription(subscription);
        }
        _store.updateResource(resource);
        //send 200 OK/202 Accepted before send notify
        if (state == SipSubscriptionStateImpl.ALLOW || state == SipSubscriptionStateImpl.TERMINATED) {
          HashMap<String, String> props = new HashMap<String, String>();
          props.put("Expire", String.valueOf(subscription.getExpires()));
          event.accept(props);
          
          if (LOG.isDebugEnabled()) {
            LOG.debug("Will send notify to " + subscription + " due to subscribe");
          }
          // send notify
          Runnable sendNotify = subscription.sendNotify();
          _notifyDispatcher.put((NotifyRequest) sendNotify);
          _store.commitTx();
        }
        else {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Decline " + subscription + " due to presence policy");
          }
          event.reject(Reason.FORBIDEN);
          _store.rollbackTx();
        }
        if (state == SipSubscriptionStateImpl.TERMINATED) {
          invalidateApplicationSession(event.getSipRequest());
        }
      }
      catch (Throwable e) {
        LOG.error("Handling subscribe error", e);
        event.reject(Reason.ERROR);
        _store.rollbackTx();
      }
      finally {
        StoreHolder.setPresenceStore(null);
      }
    }
    else {
      //TODO proxy
    }
  }

  private boolean isResponsibleFor(SIPSubscribeEvent event) {
    // TODO check domains.
    return true;
  }
  
  private void invalidateApplicationSession(final SipServletRequest req) {
    if (req.getApplicationSession().isValid()) {
      req.getApplicationSession().invalidate();
    }
  }
  
  private void acceptPublish(SIPPublishEvent event, EventSoftState eventSoftState) {
    HashMap<String, String> headers = new HashMap<String, String>();
    headers.put("SIP-ETag", eventSoftState.getEntityTag());
    headers.put("Expires", String.valueOf(eventSoftState.getSpareTime()));
    event.accept(headers);
  }
  
  private void rejectMinExpire(AcceptableEvent event, SipPresenceFactory presenceFactory) {
    HashMap<String, String> headers = new HashMap<String, String>();
    headers.put("Min-Expires", String.valueOf(presenceFactory.getSubscribeMinExpires()));
    event.reject(Reason.INTERVAL_TOO_BRIEF, headers);
  }
  
  private SIPResource getResource(SipPresenceFactory presenceFactory, String resourceUri, String eventName) {
    SIPResource resource = (SIPResource) _store.getResource(resourceUri, eventName);
    if (resource == null) {
      resource = presenceFactory.createResource(resourceUri, eventName);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Created " + resource);
      }
      _store.addResource(resource);
    }
    return resource;
  }
  
  @Override
  public void destroy() {
    _context = null;
    _notifyDispatcher.shutdown();
    _notifyDispatcher = null;
    _presenceFactory = null;
    _store.destroy();
    _store = null;
  }

  @Override
  public void run() {
    //TODO EventSoftState clean
    //TODO Subscription clean
  }

  public NotifyDispatcher getNotifyDispatcher() {
    return _notifyDispatcher;
  }

  @Override
  public String getName() {
    return SIPPresenceService.class.getName();
  }
}

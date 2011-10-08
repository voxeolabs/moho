package com.voxeo.moho.presence.impl.sip.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.voxeo.moho.event.SubscribeEvent.SubscriptionContext;
import com.voxeo.moho.presence.NotifyBody;
import com.voxeo.moho.presence.Resource;
import com.voxeo.moho.presence.SubscriptionID;
import com.voxeo.moho.presence.impl.AbstractPresenceStore;
import com.voxeo.moho.presence.impl.sip.SIPPresenceStore;
import com.voxeo.moho.presence.sip.EventSoftState;
import com.voxeo.moho.presence.sip.SIPPresenceResource;
import com.voxeo.moho.presence.sip.impl.SIPConstans;
import com.voxeo.moho.sip.SIPSubscribeEvent.SIPSubscriptionContext;

public class SIPMemoryPresenceStore extends AbstractPresenceStore implements SIPPresenceStore {
  
  
  private Map<SubscriptionID, SubscriptionContext> _subscriptions = new ConcurrentHashMap<SubscriptionID, SubscriptionContext>();
  
  //<resourceUri, <notifyBodyType, List<SubID>>>
  private Map<String, HashMap<String, ArrayList<SubscriptionID>>> _resourceVsSubcribtion = new ConcurrentHashMap<String, HashMap<String,ArrayList<SubscriptionID>>>();
  
  //Presence event
  //<resourceUri, <notifyBodyType, notifyBody>>
  private Map<String, HashMap<String, NotifyBody>> _presenceNotifyBodys = new ConcurrentHashMap<String, HashMap<String, NotifyBody>>();
  
  //<resourceUri, <eventName, Resource>>
  private Map<String, HashMap<String, Resource>> _resources = new ConcurrentHashMap<String, HashMap<String,Resource>>();
  
  //<resourceUri, <entity, EventSoftState>>
  private Map<String, HashMap<String, EventSoftState>> _softStates = new ConcurrentHashMap<String, HashMap<String,EventSoftState>>();

  @Override
  public void init(Map<String, String> props) {

  }

  @Override
  public Resource getResource(String resourceUri, String eventName) {
    HashMap<String, Resource> resources = _resources.get(resourceUri);
    if (resources != null) {
      return resources.get(eventName);
    }
    return null;
  }

  @Override
  public void destroy() {
    // TODO Auto-generated method stub

  }

  @Override
  public void addSubscription(SubscriptionContext context) {
    _subscriptions.put((SubscriptionID) context.getId(), context);
    HashMap<String, ArrayList<SubscriptionID>> bodytypeVsId = _resourceVsSubcribtion.get(context.getSubscribee());
    if (bodytypeVsId == null) {
      bodytypeVsId = new HashMap<String, ArrayList<SubscriptionID>>();
      _resourceVsSubcribtion.put(context.getSubscribee(), bodytypeVsId);
    }
    String notifyBodyType = ((SIPSubscriptionContext) context).getNotifyBodyType();
    ArrayList<SubscriptionID> idList = bodytypeVsId.get(notifyBodyType);
    if (idList == null) {
      idList = new ArrayList<SubscriptionID>();
      bodytypeVsId.put(notifyBodyType, idList);
    }
    idList.add((SubscriptionID) context.getId());
  }

  @Override
  public void updateSubscripton(SubscriptionContext context) {
    addSubscription(context);
  }

  @Override
  public void removeSubscription(SubscriptionContext context) {
    _subscriptions.remove(context.getId());
    HashMap<String, ArrayList<SubscriptionID>> bodytypeVsId = _resourceVsSubcribtion.get(context.getSubscribee());
    if (bodytypeVsId != null) {
      ArrayList<SubscriptionID> idList = bodytypeVsId.get(((SIPSubscriptionContext) context).getNotifyBodyType());
      if (idList != null) {
        idList.remove((SubscriptionID) context.getId());
      }
    }
  }
  
  public List<SubscriptionID> getSubscriptions(String resourceUri, String event, String notifyBodyType) {
    if (SIPConstans.EVENT_NAME_PRESENCE.equals(event)) {
      HashMap<String, ArrayList<SubscriptionID>> bodytypeVsId = _resourceVsSubcribtion.get(resourceUri);
      if (bodytypeVsId != null) {
        return bodytypeVsId.get(notifyBodyType);
      }
      return Collections.emptyList();
    }
    else {
      throw new IllegalArgumentException("Can't get subscriptions for event[" + event + "]");
    }
  }
  
  public SubscriptionContext getSubscription(SubscriptionID subId) {
    return _subscriptions.get(subId);
  }

  @Override
  public boolean isSubscriptionExist(SubscriptionContext context) {
    return _subscriptions.containsKey(context.getId());
  }

  @Override
  public void updateResource(Resource resource) {
    
  }
  
  public void addEventSoftState(EventSoftState state) {
    HashMap<String, EventSoftState> eventNameVsStates = _softStates.get(state.getResourceURL());
    if (eventNameVsStates == null) {
      eventNameVsStates = new HashMap<String, EventSoftState>();
      _softStates.put(state.getResourceURL(), eventNameVsStates);
    }
    eventNameVsStates.put(state.getEntityTag(), state);
  }
  
  public boolean removeEventSoftState(EventSoftState state) {
    HashMap<String, EventSoftState> eventNameVsStates = _softStates.get(state.getResourceURL());
    if (eventNameVsStates != null) {
      eventNameVsStates.remove(state.getEntityTag());
      return true;
    }
    return false;
  }
  
  public EventSoftState getEventSoftState(String resourceUri, String entityTag) {
    HashMap<String, EventSoftState> eventNameVsStates = _softStates.get(resourceUri);
    if (eventNameVsStates != null) {
      return eventNameVsStates.get(entityTag);
    }
    return null;
  }

  public NotifyBody getNotifyBody(String resourceUri, String eventName, String notifyBodyType) {
    if (SIPConstans.EVENT_NAME_PRESENCE.equals(eventName)) {
      HashMap<String, NotifyBody> bodyTyeVsNotifyBody = _presenceNotifyBodys.get(resourceUri);
      if (bodyTyeVsNotifyBody != null) {
        return bodyTyeVsNotifyBody.get(notifyBodyType);
      }
      return null;
    }
    throw new IllegalArgumentException("Can't find notify body for event[" + eventName + "]");
  }
  
  public void addNotifyBody(String resourceUri, String eventName, String notifyBodyType, NotifyBody notifyBody) {
    if (SIPConstans.EVENT_NAME_PRESENCE.equals(eventName)) {
      HashMap<String, NotifyBody> typeVsBody = _presenceNotifyBodys.get(resourceUri);
      if (typeVsBody == null) {
        typeVsBody = new HashMap<String, NotifyBody>();
        _presenceNotifyBodys.put(resourceUri, typeVsBody);
      }
      typeVsBody.put(notifyBodyType, notifyBody);
    }
    else {
      throw new IllegalArgumentException("Can't save notify body for event[" + eventName + "]");
    }
  }
  
  @Override
  public void startTx() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void commitTx() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void rollbackTx() {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void addResource(Resource resource) {
    if (resource instanceof SIPPresenceResource) {
      SIPPresenceResource res = (SIPPresenceResource) resource;
      HashMap<String, Resource> uriVsRes = _resources.get(res.getUri());
      if (uriVsRes == null) {
        uriVsRes = new HashMap<String, Resource>();
        _resources.put(res.getUri(), uriVsRes);
      }
      uriVsRes.put(res.getEventName(), res);
    }
    else {
      throw new IllegalArgumentException("Can't save resource for type[" + resource.getClass().getSimpleName() + "]");
    }
  }
}

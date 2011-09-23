package com.voxeo.moho.presence.sip.impl;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipSession;

import org.apache.log4j.Logger;

import com.voxeo.moho.presence.NotifyBody;
import com.voxeo.moho.presence.PresenceStore;
import com.voxeo.moho.presence.Resource;
import com.voxeo.moho.presence.impl.StoreHolder;
import com.voxeo.moho.sip.SIPSubscribeEvent.SIPSubscriptionContext;

public class NotifyRequest implements Runnable {
  // Cseq number in a request

  protected static final Logger LOG = Logger.getLogger(NotifyRequest.class);
  
  private static final int TRY_TIMES = 10;
  
  private static final int SLEEP_TIMES = 500;

  protected int m_sentReqCount;

  //composite id of Subscription : m_subID.getSessionId() + ":" + m_subID.getEventId()
  private SIPSubscriptionContext _subscription = null;
  
  private String m_id;
  
  protected String _contentType = null;
  
  protected long m_lastNotifyTime;
  
  protected long m_throttlingRate;
  
  private NotifyBody _notifyBody;
  
  private PresenceStore _presenceStore;
  
  public NotifyRequest() {
    
  }
    
  public NotifyRequest(SIPSubscriptionContext subscription, PresenceStore presenceStore) {
    _presenceStore = presenceStore;
    _subscription = subscription;
  }

  public long getLastNotifyTime() {
    return m_lastNotifyTime;
  }

  public void setLastNotifyTime(long lastNotifyTime) {
    this.m_lastNotifyTime = lastNotifyTime;
  }
  
  public String getId() {
    return m_id;
  }
  
  public void setId(String id) {
    this.m_id = id;
  }
  
  public SIPSubscriptionContext getSubscription() {
    return _subscription;
  }

  public void setSubscription(SIPSubscriptionContext sub) {
    _subscription = sub;
  }
  
  /**
   * wether it is a valid notify to send  
   * @param session
   * @return
   */
  public boolean isValid() {
    try {
      SIPSubscriptionContext subscription = getSubscription();
      if (subscription == null) {
        LOG.warn("subscription does not exist. It might be deleted becaused of expired.");
        return false;
      }
      if (subscription.getDialog() == null) {
        return false;
      }
    }
    catch (Throwable t) {
      LOG.error("Can not find relevant Subscription record.", t);
      return false;
    }
    return true;
  }
  
  public void run() {
    StoreHolder.setPresenceStore(_presenceStore);
    for (int i = 1; i <= TRY_TIMES; i++) {
      try {
        runit(i);
        break;
      }
      catch (Exception e) {
        if (i == TRY_TIMES) {
          LOG.error("Server is still busy for sending a notify after tried " + TRY_TIMES + " times!", e);
          return;
        }
        else {
          if (i == 1) {
            LOG.warn("tried " + i + " times sending a notify. ", e);
          }
          else {
            LOG.warn("tried " + i + " times sending a notify. " + e);
          }
          try {
            Thread.sleep(SLEEP_TIMES);
          }
          catch (InterruptedException e1) {
            ;
          }
          continue;
        }
      }
      catch (Throwable t) {
        LOG.error("Server Error sending a notify. ", t);
      }
      finally {
        StoreHolder.setPresenceStore(null);
      }
    }
  }
  
  public void runit(int times) {
    SIPSubscriptionContextImpl subscription = (SIPSubscriptionContextImpl) getSubscription();
    SipSession session = subscription.getDialog();
    
    try {
      SipServletRequest notify = session.createRequest("NOTIFY");
      PresenceStore presenceStore = StoreHolder.getPresenceStore();
      Resource resource = presenceStore.getResource(subscription.getSubscribee(), subscription.getEventName());
      NotifyBody notifyBody = resource.getNotifyBody(subscription.getNotifyBodyType());
      notify.setContent(notifyBody.getContent(), notifyBody.getName());
      notify.setHeader("Event", subscription.getEventName());
      notify.setHeader("Subscription-State", subscription.getState().getPhrase() + ";expires=" + subscription.getSpareTime());
      notify.send();
    }
    catch (Throwable e) {
      e.printStackTrace();
    }
  }
  
  public void addSentReqCount() {
    setSentReqCount(getSentReqCount() + 1);
  }

  public int getSentReqCount() {
    return m_sentReqCount;
  }
  public void setSentReqCount(int num) {
    m_sentReqCount = num;
  }

  public void setContentType(String s) {
    _contentType = s;
  }

  public String getContentType() {
    return _contentType;
  }

  public NotifyBody getNotifyBody() {
    return _notifyBody;
  }

  public void setNotifyBody(NotifyBody body) {
    _notifyBody = body;
  }


//  public NotifyBody getPreNotifyBody() {
//  }
//
//  public void setPreNotifyBody(NotifyBody body) {
//  }
}

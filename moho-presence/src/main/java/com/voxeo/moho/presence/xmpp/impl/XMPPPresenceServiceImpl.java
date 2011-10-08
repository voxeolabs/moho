package com.voxeo.moho.presence.xmpp.impl;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import com.voxeo.moho.presence.Resource;
import com.voxeo.moho.presence.StoreRetrieveListener;
import com.voxeo.moho.presence.impl.StoreHolder;
import com.voxeo.moho.presence.impl.xmpp.XMPPPresenceStore;
import com.voxeo.moho.presence.impl.xmpp.memory.XMPPMemoryPresenceStore;
import com.voxeo.moho.presence.xmpp.Roster;
import com.voxeo.moho.presence.xmpp.RosterService;
import com.voxeo.moho.presence.xmpp.XMPPPresenceResource;
import com.voxeo.moho.presence.xmpp.XMPPPresenceService;
import com.voxeo.moho.presence.xmpp.XmppPendingNotification;
import com.voxeo.moho.spi.ExecutionContext;
import com.voxeo.moho.xmpp.RosterEvent.Ask;
import com.voxeo.moho.xmpp.RosterEvent.RosterItem;
import com.voxeo.moho.xmpp.RosterEvent.XmppSubscription;
import com.voxeo.moho.xmpp.RosterGet;
import com.voxeo.moho.xmpp.RosterSet;
import com.voxeo.moho.xmpp.XMPPAcceptableEvent.Reason;
import com.voxeo.moho.xmpp.XMPPEndpoint;
import com.voxeo.moho.xmpp.XMPPIQEvent;
import com.voxeo.moho.xmpp.XMPPMessageEvent;
import com.voxeo.moho.xmpp.XMPPPresenceEvent;
import com.voxeo.servlet.xmpp.JID;
import com.voxeo.servlet.xmpp.PresenceMessage;
import com.voxeo.servlet.xmpp.XmppFactory;
import com.voxeo.servlet.xmpp.XmppServlet;
import com.voxeo.servlet.xmpp.XmppSession;
import com.voxeo.servlet.xmpp.XmppSessionsUtil;

/**
 * TODO 8.1. General Considerations(http://tools.ietf.org/html/rfc6121#page-78)
 */
public class XMPPPresenceServiceImpl implements XMPPPresenceService {

  private static final Logger LOG = Logger.getLogger(XMPPPresenceServiceImpl.class);

  private ExecutionContext _context;

  private RosterService _rosterService;

  private XMPPPresenceStore _store;

  private XmppSessionsUtil _sessionUtil;

  private XmppFactory _xmppFactory;

  @Override
  public void init(ExecutionContext context, Map<String, String> properties) throws Exception {
    _rosterService = new RosterServiceImpl(this);
    _context = context;
    _sessionUtil = (XmppSessionsUtil) _context.getServletContext().getAttribute(XmppServlet.SESSIONUTIL);
    _xmppFactory = context.getXmppFactory();
    String storeImpl = properties.get(STORE_IMPL);
    if (storeImpl == null) {
      storeImpl = XMPPMemoryPresenceStore.class.getName();
    }
    try {
      _store = (XMPPPresenceStore) Class.forName(storeImpl).newInstance();
      _store.init(properties);
      _store.addRetrieveListener(XmppPendingNotification.class, new StoreRetrieveListener<XmppPendingNotification>() {
        @Override
        public void onRetrieve(XmppPendingNotification resource) {
          resource.setExecutionContext(_context);
        }
      });
      _store.addRetrieveListener(Resource.class, new StoreRetrieveListener<Resource>() {
        @Override
        public void onRetrieve(Resource resource) {
          resource.setExecutionContext(_context);
        }
      });
    }
    catch (Exception e) {
      throw new IllegalArgumentException("Invalidate Presence Store implementation: " + e);
    }
    XmppPresenceServiceRegistrar.registerService(this);
  }

  @Override
  public void destroy() {
    _rosterService = null;
    _store.destroy();
    _store = null;
  }

  @Override
  public String getName() {
    return XMPPPresenceService.class.getName();
  }

  @Override
  public void doPresence(XMPPPresenceEvent event) {
    String type = event.getXmppRequest().getType();
    try {
      StoreHolder.setPresenceStore(_store);
      // presence publish
      if (isEmpty(type) || XMPPPresenceEvent.TYPE_UNAVAILABLE.equals(type)) {
        doPresencePublish(event);
        return;
      }
      // subscription
      JID to = event.getXmppRequest().getTo();
      if (isOutbound(event)) {
        doOutboundSubscription(to, event);
      }
      else {
        if (XMPPPresenceEvent.TYPE_PROBE.equals(event.getType())) {
          doInboundPresenceProbe(event);
        }
        else {
          doInboundSubscription(to, event);
        }
      }
    }
    catch (Exception e) {
      LOG.error("Error handling " + event, e);
    }
    finally {
      StoreHolder.setPresenceStore(null);
    }
  }

  /**
   * Check if the given string is empty.
   * 
   * @param string
   *          String to check
   * @return True if string is empty
   */
  public static boolean isEmpty(final String string) {
    if (string == null || string.trim().length() == 0 || string.equals(EMPTY)) {
      return true;
    }
    return false;
  }

  public static final String EMPTY = "";

  private void doPresencePublish(XMPPPresenceEvent event) throws IOException, ServletException {
    JID from = event.getXmppRequest().getFrom();
    XMPPPresenceResource resource = getResource(from);
    if (!resource.isAvailable() && isEmpty(event.getType())) {
      // After a connected resource
      // sends initial presence (see Section 4.2), it is referred to as an
      // "available resource".
      resource.setAvailable(true);
      Roster roster = _rosterService.getRoster(from.getBareJID());
      // send presence probe to all subscribees
      if (roster != null) {
        for (RosterItem itm : roster.getItems()) {
          if (itm.getSubscription() == XmppSubscription.BOTH || itm.getSubscription() == XmppSubscription.TO) {
            try {
              sendPresenceProbe(from, _xmppFactory.createJID(itm.getJID()));
            }
            catch (Exception e) {
              LOG.warn("Error sending presence probe from " + from + " to " + itm.getJID(), e);
            }
          }
        }
      }
    }
    else if (resource.isAvailable() && event.getType().equalsIgnoreCase(XMPPPresenceEvent.TYPE_UNAVAILABLE)) {
      resource.setAvailable(false);
      resource.setInterested(false);
    }
    if (isOutbound(event)) {
      Roster roster = _rosterService.getRoster(from.getBareJID());
      // broadcast to all subscribers
      if (roster != null) {
        for (RosterItem itm : roster.getItems()) {
          if (itm.getSubscription() == XmppSubscription.BOTH || itm.getSubscription() == XmppSubscription.FROM) {
            sendPresence(from, _xmppFactory.createJID(itm.getJID()), event.getType(), event.getContent());
          }
        }
      }
      // //broadcast to all of the user's available
      // resources, including the resource that generated the presence
      // notification in the first place
      sendPresence(from, from, event.getType(), event.getContent());
    }
    else {
      sendPresence(from, event.getMessageTo(), event.getType(), event.getContent());
    }
  }

  public void sendPresenceProbe(JID from, JID to) throws ServletException, IOException {
    PresenceMessage probePresence = _xmppFactory.createPresence(from, to, XMPPPresenceEvent.TYPE_PROBE, (Element[]) null);
    probePresence.send();
  }

  public XmppSessionsUtil getSessionUtil() {
    return _sessionUtil;
  }

  public XmppFactory getXmppFactory() {
    return _xmppFactory;
  }

  private void doInboundPresenceProbe(XMPPPresenceEvent event) throws IOException, ServletException {
    JID contact = event.getMessageTo();
    Roster roster = _rosterService.getRoster(contact);
    RosterItem item = roster.getItem(event.getMessageTo().toString());
    if (!isUserExist(contact)
        || (item.getSubscription() != XmppSubscription.BOTH && item.getSubscription() != XmppSubscription.FROM)) {
      event.getXmppRequest().getSession().createPresence(null, XMPPPresenceEvent.TYPE_UNSUBSCRIBED, (Element[]) null).send();
    }
    else if (isUserMoved(contact)) {
      // handle contact has moved temporarily or permanently to
      // another address
    }
    else if (!isUserAvailable(contact)) {
      sendPresence(contact.getBareJID(), event.getMessageFrom(), XMPPPresenceEvent.TYPE_UNAVAILABLE, null);
    }
    else {
      sendPresence(contact, event.getMessageFrom(), null, null);
    }
  }

  private boolean isUserMoved(JID contact) {
    return false;
  }

  private void doOutboundSubscription(JID to, XMPPPresenceEvent event) throws ServletException, IOException {
    // subscribe
    if (XMPPPresenceEvent.TYPE_SUBSCRIBE.equalsIgnoreCase(event.getType())) {
      if (to.getResource() != null) {
        event.getXmppRequest().setTo(to.getBareJID());
      }
      if (event.getMessageFrom().getResource() != null) {
        event.getXmppRequest().setFrom(event.getMessageFrom().getBareJID());
      }
      if (isUserInHost(to)) {
        doInboundSubscription(to, event);
      }
      else {
        routeOutboundPresence(event);
      }
      // ask='subscribe'
      // jid='juliet@example.com'
      // subscription='none'
      RosterItem rosterItem = _rosterService.getRosterItem(event.getMessageFrom().toString(), to.toString());
      if (rosterItem == null) {
        rosterItem = _rosterService.createRosterItem(to.toString());
      }
      rosterItem.setAsk(Ask.SUBSCRIBE);
      _rosterService.addRosterItem(event.getMessageFrom(), rosterItem);
    }
    // approval(from contact to user on contact's server)
    else if (XMPPPresenceEvent.TYPE_SUBSCRIBED.equalsIgnoreCase(event.getType())) {
      if (event.getMessageFrom().getResource() != null) {
        event.getXmppRequest().setFrom(event.getMessageFrom().getBareJID());
      }
      if (isUserInHost(to)) {
        doInboundSubscription(to, event);
      }
      else {
        routeOutboundPresence(event);
      }
      // to contact's interested resources
      RosterItem rosterItem = _rosterService.getRosterItem(event.getMessageFrom(), to);
      if (rosterItem == null) {
        rosterItem = _rosterService.createRosterItem(to);
        rosterItem.setSubscription(XmppSubscription.FROM);
      }
      else {
        if (isSubscriptionExist(to.toString(), event.getMessageFrom().toString())) {
          rosterItem.setSubscription(XmppSubscription.BOTH);
        }
        else {
          rosterItem.setSubscription(XmppSubscription.FROM);
        }
      }
      rosterItem.setAsk(Ask.NONE);
      _rosterService.addRosterItem(event.getMessageFrom(), rosterItem);
      sendPresence(event.getMessageFrom(), _xmppFactory.createJID(rosterItem.getJID()), null, null);
      cancelPendingNotification(to, event.getMessageFrom(), XMPPPresenceEvent.TYPE_SUBSCRIBE);
    }
    // cancellation
    else if (XMPPPresenceEvent.TYPE_UNSUBSCRIBED.equalsIgnoreCase(event.getType())) {
      // get contact's roster
      Roster roster = _rosterService.getRoster(event.getMessageFrom().getBareJID());
      RosterItem rosterItem = roster.getItem(event.getMessageTo().toString());
      if (rosterItem == null
          || (rosterItem.getSubscription() == XmppSubscription.NONE && (rosterItem.getAsk() == Ask.NONE || rosterItem
              .getAsk() == Ask.SUBSCRIBE)) || rosterItem.getSubscription() == XmppSubscription.TO) {
        // do not deliver presence or presence notifications
      }
      else if (((rosterItem.getSubscription() == XmppSubscription.NONE && (rosterItem.getAsk() == Ask.NONE || rosterItem
          .getAsk() == Ask.SUBSCRIBE)) || rosterItem.getSubscription() == XmppSubscription.TO)
          && isApproved(event.getMessageFrom(), event.getMessageTo())) {
        // remove pre-approval
      }
      else {
        // route or deliver both presence notifications of type
        // "unavailable" and presence stanzas of type "unsubscribed" to the
        // user and MUST send a roster push to the contact.
        sendPresence(event.getMessageFrom().getBareJID(), event.getMessageTo(), XMPPPresenceEvent.TYPE_UNAVAILABLE, null);
        sendPresence(event.getMessageFrom().getBareJID(), event.getMessageTo(), XMPPPresenceEvent.TYPE_UNSUBSCRIBED, null);
        if (rosterItem.getSubscription() == XmppSubscription.BOTH) {
          rosterItem.setSubscription(XmppSubscription.TO);
        }
        else {
          rosterItem.setSubscription(XmppSubscription.NONE);
        }
        _rosterService.addRosterItem(to, rosterItem);
      }
      cancelPendingNotification(to, event.getMessageFrom(), XMPPPresenceEvent.TYPE_SUBSCRIBE);
    }
    // unsubscribe
    else if (XMPPPresenceEvent.TYPE_UNSUBSCRIBE.equalsIgnoreCase(event.getType())) {
      if (event.getMessageFrom().getResource() != null) {
        event.getXmppRequest().setFrom(event.getMessageFrom().getBareJID());
      }
      // bare from and to
      if (isUserInHost(to)) {
        doInboundSubscription(to, event);
      }
      else {
        routeOutboundPresence(event);
      }
      Roster roster = _rosterService.getRoster(event.getMessageFrom());
      RosterItem rosterItem = roster.getItem(event.getMessageTo().toString());
      // The user's server then MUST send a roster push with the updated
      // roster item to all of the user's interested resources, where the
      // subscription state is now either "none" or "from"
      if (rosterItem.getSubscription() == XmppSubscription.BOTH) {
        rosterItem.setSubscription(XmppSubscription.FROM);
      }
      else {
        rosterItem.setSubscription(XmppSubscription.NONE);
      }
      _rosterService.addRosterItem(event.getMessageFrom(), rosterItem);
    }
  }

  private boolean isApproved(JID messageFrom, JID messageTo) {
    return false;
  }

  private void routeOutboundPresence(XMPPPresenceEvent event) throws ServletException, IOException {
    List<Element> contents = event.getXmppRequest().getElements();
    sendPresence(event.getMessageFrom(), event.getMessageTo(), event.getType(), contents);
    // TODO
    /*
     * If a remote contact does not approve or deny the subscription request
     * within some configurable amount of time, the user's server SHOULD resend
     * the subscription request to the contact based on an
     * implementation-specific algorithm (e.g., whenever a new resource becomes
     * available for the user, or after a certain amount of time has elapsed);
     * this helps to recover from transient, silent errors that might have
     * occurred when the original subscription request was routed to the remote
     * domain. When doing so, it is RECOMMENDED for the server to include an
     * 'id' attribute so that it can track responses to the resent subscription
     * request.
     */
  }

  private boolean isUserInHost(JID jid) {
    return jid.getDomain().equals("voxeo.com");
  }

  private boolean isOutbound(XMPPPresenceEvent event) {
    if (event.getMessageFrom().getDomain().equalsIgnoreCase("voxeo.com")) {
      return true;
    }
    else {
      return false;
    }
  }

  private void doInboundSubscription(JID to, XMPPPresenceEvent event) throws IOException, ServletException {
    if (XMPPPresenceEvent.TYPE_SUBSCRIBE.equalsIgnoreCase(event.getType())) {
      // TODO check pre-approve
      if (isUserExist(to)) {
        if (isSubscriptionExist(event.getMessageFrom().toString(), to.toString())) {
          sendPresence(event.getMessageFrom().getBareJID(), to.getBareJID(), XMPPPresenceEvent.TYPE_SUBSCRIBED, null);
        }
        else {
          if (isUserAvailable(to)) {
            sendPresence(event.getMessageFrom(), to, event.getType(), null);
          }
          else {
            // save this subscription and trigger send when any contact is
            // available until the contact either approves or denies
            // the request
            pendingNotification(event);
          }
        }
      }
      else {
        LOG.error("Can't find user[" + to + "] for subscription from " + event.getMessageFrom());
      }
    }
    // approval
    else if (XMPPPresenceEvent.TYPE_SUBSCRIBED.equalsIgnoreCase(event.getType())) {
      Roster roster = _rosterService.getRoster(event.getMessageTo());
      if (roster != null && roster.getItem(event.getMessageFrom().toString()) != null) {
        RosterItem rosterItem = roster.getItem(event.getMessageFrom().toString());
        if (rosterItem.getSubscription() == XmppSubscription.NONE || (rosterItem.getSubscription() == XmppSubscription.FROM && rosterItem.getAsk() == Ask.SUBSCRIBE)) {
          // Deliver the inbound subscription approval to all of the user's
          // interested resources
          try {
            List<XmppSession> sessions = _sessionUtil.getSessions(event.getMessageTo().getBareJID());
            for (XmppSession session : sessions) {
              if (isResourceInterested(session.getRemoteJID())) {
                session.createPresence(event.getMessageFrom().toString(), event.getType(), (Element[]) null).send();
              }
            }
          }
          catch (Exception e) {
            LOG.error("Error do roster push", e);
          }
          // roster push to all of the user's interested resources
          // 'subscription' attribute set to a value of "to" (if the
          // subscription state was "None + Pending Out" or "None + Pending
          // Out+In") or "both" (if the subscription state was "From + Pending
          // Out").
          if (rosterItem.getSubscription().equals(XmppSubscription.NONE)) {
            rosterItem.setSubscription(XmppSubscription.TO);
          }
          else {
            rosterItem.setSubscription(XmppSubscription.BOTH);
          }
          rosterItem.setAsk(Ask.NONE);
          _rosterService.addRosterItem(event.getMessageTo(), rosterItem);
          
          // deliver the available presence stanza
          // received from each of the contact's available resources to each
          // of the user's available resources.
          JID fromJID = _xmppFactory.createJID(rosterItem.getJID());
          if (isUserAvailable(to)) {
            sendPresence(fromJID, to, null, null);
          }
          //save notification if no available resources for user and deliver it
          // later
          else {
            pendingNotification(event);
          }
        }
      }
      else {
        // silent ignore
      }
    }
    // cancellation
    else if (XMPPPresenceEvent.TYPE_UNSUBSCRIBED.equalsIgnoreCase(event.getType())) {
      // user's roster
      Roster roster = _rosterService.getRoster(event.getMessageTo());
      if (roster != null) {
        RosterItem rosterItem = roster.getItem(event.getMessageFrom().toString());
        if (rosterItem != null
            && (rosterItem.getSubscription() == XmppSubscription.BOTH || rosterItem.getSubscription() == XmppSubscription.TO)) {
          if (isUserAvailable(event.getMessageTo())) {
            sendPresence(event.getMessageFrom(), event.getMessageTo(), event.getType(), null);
            sendPresence(event.getMessageFrom(), event.getMessageTo(), XMPPPresenceEvent.TYPE_UNAVAILABLE, null);
            // Initiate a roster push to all of the user's interested resources
            if (rosterItem.getSubscription() == XmppSubscription.BOTH) {
              rosterItem.setSubscription(XmppSubscription.FROM);
            }
            else {
              rosterItem.setSubscription(XmppSubscription.NONE);
            }
            _rosterService.addRosterItem(event.getMessageTo(), rosterItem);
          }
          // save notification if no available resources for user and
          // deliver it later
          else {
            pendingNotification(event);
          }
        }
      }
      else {
        // silent ignore
      }
    }
    // unsubscribe
    else if (XMPPPresenceEvent.TYPE_UNSUBSCRIBE.equalsIgnoreCase(event.getType())) {
      // get contact's roster
      Roster roster = _rosterService.getRoster(event.getMessageTo());
      RosterItem rosterItem = roster.getItem(event.getMessageFrom());
      if (rosterItem.getSubscription() == XmppSubscription.BOTH
          || rosterItem.getSubscription() == XmppSubscription.FROM) {
        routeOutboundPresence(event);

        // Initiate a roster push to all of the contact's interested
        // resources, containing an updated roster item for the user with
        // the 'subscription' attribute set to a value of "none" (if the
        // subscription state was "From" or "From + Pending Out") or "to"
        // (if the subscription state was "Both").
        if(rosterItem.getSubscription() == XmppSubscription.BOTH){
          rosterItem.setSubscription(XmppSubscription.TO);
        }
        else if (rosterItem.getSubscription() == XmppSubscription.FROM){
          rosterItem.setSubscription(XmppSubscription.NONE);
        }
        _rosterService.addRosterItem(to, rosterItem);
        
        if (isUserAvailable(to)) {
          sendPresence(event.getMessageFrom(), to, XMPPPresenceEvent.TYPE_UNAVAILABLE, null);
        }
        else {
          pendingNotification(event);
        }
      }
      else {
        // If the contact's
        // server is keeping track of an inbound presence subscription request
        // from the user to the contact but the user is not yet in the contact's
        // roster (functionally equivalent to a subscription state of "None +
        // Pending In" where the contact never added the user to the contact's
        // roster), then the contact's server MUST simply remove any record of
        // the inbound presence subscription request (it cannot remove the user
        // from the contact's roster because the user was never added to the
        // contact's roster).
      }
    }
  }

  private void pendingNotification(XMPPPresenceEvent event) {
    pendingNotification(event.getMessageFrom().toString(), event.getMessageTo().toString(), event.getType(), event.getContent());
  }
  
  private void pendingNotification(String from, String to, String type, List<Element> content) {
    XmppPendingNotification notification = getNotification(from, to);
    notification.setPresenceStanza(content);
    notification.setType(to);
    _store.addNotification(notification);
  }
  
  private void cancelPendingNotification(JID from, JID to, String typeSubscribe) {
    XmppPendingNotification notify = (XmppPendingNotification) _store.getNotification(from.toString(), to.toString());
    if (notify != null && typeSubscribe.equals(notify.getType())) {
      _store.removeNotification(notify);
    }
  }

  private void sendPresence(JID from, JID to, String type, List<Element> contents) throws ServletException, IOException {
    if (isResourceAvailable(to) || XMPPPresenceEvent.TYPE_UNAVAILABLE.equalsIgnoreCase(type)) {
      _xmppFactory.createPresence(from.getBareJID(), to, type,
          contents == null ? null : contents.toArray(new Element[contents.size()])).send();
    }
  }
  
  private void sendPresence(String from, String to, String type, List<Element> contents) throws ServletException, IOException {
    sendPresence(_xmppFactory.createJID(from), _xmppFactory.createJID(to), type, contents);
  }

  @Override
  public void doIQ(XMPPIQEvent event) {
    // simply accept session request
    if (event.getElement("session") != null) {
      Element resElement = null;
      try {
        resElement = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
            .createElementNS("urn:ietf:params:xml:ns:xmpp-session", "session");
      }
      catch (Exception e) {
        event.reject(Reason.INTERNAL_SERVER_ERROR, e.getMessage());
        return;
      }
      event.accept(resElement);
      return;
    }
    try {
      StoreHolder.setPresenceStore(_store);
      // roster get
      if (RosterGet.class.isAssignableFrom(event.getClass())) {
        doRosterGet((RosterGet) event);
      }
      // roster set
      else if (RosterSet.class.isAssignableFrom(event.getClass())) {
        doRosterSet((RosterSet) event);
      }
      //
      else {
        event.reject(Reason.FEATURE_NOT_IMPLEMENTED, "Unsupported event " + event);
      }
    }
    catch (Throwable e) {
      LOG.error("Error handling " + event, e);
    }
    finally {
      StoreHolder.setPresenceStore(null);
    }
  }

  public void doMessage(XMPPMessageEvent event) {
    try {
      ((XMPPEndpoint) (event.getTo())).sendRichContent(event.getFrom(), event.getContent());
    }
    catch (IOException e) {
      LOG.error("Send message error " + event.getXmppRequest(), e);
    }
  }

  private void doRosterSet(RosterSet event) {
    if (event.getItems().size() != 1) {
      event.reject(Reason.BAD_REQUEST, "Item size can't be " + event.getItems().size());
      return;
    }
    RosterItem item = event.getItems().iterator().next();
    // remove roster item
    if (item.getSubscription() == XmppSubscription.REMOVE) {
      Roster roster = _rosterService.getRoster(event.getMessageFrom().getBareJID());
      if (roster == null || roster.getItem(item.getJID()) == null) {
        event.reject(Reason.ITEM_NOT_FOUND);
        return;
      }
      RosterItem oldItem = roster.getItem(item.getJID());
      _rosterService.removeRosterItem(event.getMessageFrom().getBareJID().toString(), item);
      try {
        if (oldItem.getSubscription() == XmppSubscription.BOTH || oldItem.getSubscription() == XmppSubscription.TO) {
          if (isUserAvailable(oldItem.getJID())) {
            sendPresence(event.getMessageFrom().toString(), oldItem.getJID(), XMPPPresenceEvent.TYPE_UNSUBSCRIBE, null);
          }
          else {
            pendingNotification(event.getMessageFrom().toString(), oldItem.getJID(), XMPPPresenceEvent.TYPE_UNSUBSCRIBE, null);
          }
        }
        if (oldItem.getSubscription() == XmppSubscription.BOTH || oldItem.getSubscription() == XmppSubscription.FROM) {
          if (isUserAvailable(event.getMessageFrom())) {
            sendPresence(oldItem.getJID(), event.getMessageFrom().toString(), XMPPPresenceEvent.TYPE_UNSUBSCRIBED, null);
          }
          else {
            pendingNotification(oldItem.getJID(), event.getMessageFrom().toString(), XMPPPresenceEvent.TYPE_UNSUBSCRIBED, null);
          }
        }
      }
      catch (Exception e) {
        LOG.error("Error do presence unsubscribed send due to roster remove", e);
      }
    }
    else {
      _rosterService.addRosterItem(event.getMessageFrom().getBareJID().toString(), item);
    }
    event.accept();
  }

  private XmppPendingNotification createNotification(String from, String to) {
    return new XmppPendingNotificationImpl(_context, from, to);
  }
  
  private XmppPendingNotification getNotification(String from, String to) {
    XmppPendingNotification subscription = (XmppPendingNotification) _store.getNotification(from, to);
    if (subscription == null) {
      subscription = createNotification(from, to);
    }
    return subscription;
  }

  public boolean isSubscriptionExist(String from, String to) {
    Roster roster = _rosterService.getRoster(from);
    if (roster != null) {
      RosterItem item = roster.getItem(to);
      if (item != null
          && (item.getSubscription() == XmppSubscription.BOTH || item.getSubscription() == XmppSubscription.TO)) {
        return true;
      }
    }
    roster = _rosterService.getRoster(to);
    if (roster != null) {
      RosterItem item = roster.getItem(from);
      if (item != null
          && (item.getSubscription() == XmppSubscription.BOTH || item.getSubscription() == XmppSubscription.FROM)) {
        return true;
      }
    }
    return false;
  }
  
  public boolean isSubscriptionExists(JID from, JID to) {
    return isSubscriptionExist(from.toString(), to.toString());
  }
  
  //whether has available resources for this user
  private boolean isUserAvailable(JID to) {
    return isResourceAvailable(to.getBareJID());
  }
  
  private boolean isUserAvailable(String to) {
    return isUserAvailable(_xmppFactory.createJID(to));
  }
  
  private boolean isUserExist(JID to) {
    return true;
  }
  
  public boolean isResourceAvailable(JID jid) {
    if (jid.getResource() == null) {
      List<XMPPPresenceResource> resources = _store.getResourceByBareID(jid.toString());
      for (XMPPPresenceResource res : resources) {
        if (res.isAvailable()) {
          return true;
        }
      }
    }
    XMPPPresenceResource resource = getResource(jid);
    if (resource != null) {
      return resource.isAvailable();
    }
    return false;
  }
  
  public boolean isResourceInterested(JID jid) {
    if (jid.getResource() == null) {
      List<XMPPPresenceResource> resources = _store.getResourceByBareID(jid.toString());
      for (XMPPPresenceResource res : resources) {
        if (res.isInterested()) {
          return true;
        }
      }
    }
    XMPPPresenceResource resource = getResource(jid);
    if (resource != null) {
      return resource.isInterested();
    }
    return false;
  }
  
  private XMPPPresenceResource getResource(JID jid) {
    XMPPPresenceResource resource = _store.getResource(jid.toString());
    if (resource == null) {
      resource = new XMPPPresenceResourceImpl(_context, jid);
    }
    return resource;
  }
  
  private void doRosterGet(RosterGet event) {
    if (event.getItems().size() != 0) {
      event.reject(Reason.BAD_REQUEST, "roster get should not contain items");
    }
    try {
      Roster roster = _rosterService.getRoster(event.getMessageFrom().getBareJID());
      if (roster != null) {
        Element results = _rosterService.createRosterItemElement(roster == null ? new RosterItem[] {} : roster.getItems().toArray(
            new RosterItem[roster.getItems().size()]));
        event.accept(results);
      }
      else {
        event.reject(Reason.ITEM_NOT_FOUND, "Can't find roster for " + event.getMessageFrom());
      }
      // If a connected resource or available resource
      // requests the roster, it is referred to as an "interested resource".
      XMPPPresenceResource resource = getResource(event.getMessageFrom());
      if (!resource.isInterested()) {
        resource.setInterested(true);
      }
    }
    catch (Exception e) {
      LOG.error("", e);
      event.reject(Reason.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }
  
  public RosterService getRosterService() {
    return _rosterService;
  }

  public XMPPPresenceStore getStore() {
    return _store;
  }
}

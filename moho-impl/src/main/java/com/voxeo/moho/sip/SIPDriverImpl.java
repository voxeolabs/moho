/**
 * Copyright 2010-2011 Voxeo Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.voxeo.moho.sip;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.media.mscontrol.MsControlFactory;
import javax.sdp.SdpFactory;
import javax.servlet.ServletException;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

import org.apache.log4j.Logger;

import com.voxeo.moho.Call;
import com.voxeo.moho.Configuration;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.Framework;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.Participant;
import com.voxeo.moho.State;
import com.voxeo.moho.Subscription;
import com.voxeo.moho.common.event.MohoInputDetectedEvent;
import com.voxeo.moho.event.CallEvent;
import com.voxeo.moho.event.EventSource;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent.Cause;
import com.voxeo.moho.event.NotifyEvent;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.event.PublishEvent;
import com.voxeo.moho.event.RegisterEvent;
import com.voxeo.moho.event.RequestEvent;
import com.voxeo.moho.event.SubscribeEvent;
import com.voxeo.moho.event.TextEvent;
import com.voxeo.moho.event.UnknownRequestEvent;
import com.voxeo.moho.reg.Registration;
import com.voxeo.moho.remote.sipbased.Constants;
import com.voxeo.moho.remote.sipbased.RemoteJoinIncomingCall;
import com.voxeo.moho.spi.ExecutionContext;
import com.voxeo.moho.spi.SIPDriver;
import com.voxeo.moho.spi.SpiFramework;
import com.voxeo.moho.util.SessionUtils;

public class SIPDriverImpl implements SIPDriver {

  private static final Logger LOG = Logger.getLogger(SIPDriverImpl.class);

  protected Framework _app = null;

  protected SipFactory _sipFacory;

  protected SdpFactory _sdpFactory;

  protected MsControlFactory _mscFactory;

  protected SipServlet _servlet;

  protected static final String[] SCHEMAS = new String[] {"sip", "tel", "sips", "<sip", "<tel", "<sips", "fax", "<fax:"};

  @Override
  public void init(SpiFramework framework) {
    _app = framework;
    _sipFacory = framework.getExecutionContext().getSipFactory();
    _sdpFactory = framework.getExecutionContext().getSdpFactory();
    _mscFactory = framework.getExecutionContext().getMSFactory();
    _servlet = framework.getSIPController();
  }

  @Override
  public void destroy() {

  }

  @Override
  public void doRequest(final SipServletRequest req) throws ServletException, IOException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Received request:" + req.toString().replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n"));
    }
    
    final String s = req.getMethod();
    if ("INVITE".equals(s)) {
      doInvite(req);
    }
    else if ("ACK".equals(s)) {
      doAck(req);
    }
    else if ("OPTIONS".equals(s)) {
      doOptions(req);
    }
    else if ("BYE".equals(s)) {
      doBye(req);
    }
    else if ("CANCEL".equals(s)) {
      doCancel(req);
    }
    else if ("REGISTER".equals(s)) {
      doRegister(req);
    }
    else if ("SUBSCRIBE".equals(s)) {
      doSubscribe(req);
    }
    else if ("NOTIFY".equals(s)) {
      doNotify(req);
    }
    else if ("MESSAGE".equals(s)) {
      doMessage(req);
    }
    else if ("INFO".equals(s)) {
      doInfo(req);
    }
    else if ("UPDATE".equals(s)) {
      doUpdate(req);
    }
    else if ("REFER".equals(s)) {
      doRefer(req);
    }
    else if ("PUBLISH".equals(s)) {
      doPublish(req);
    }
    else if ("PRACK".equals(s)) {
      doPrack(req);
    }
    else {
      doOthers(req);
    }
  }

  protected void doInvite(final SipServletRequest req) throws ServletException, IOException {
    if (req.isInitial()) {
      // process remote join
      if (req.getHeader(Constants.x_Join_Direction) != null && req.getHeader(Constants.x_Join_Direction).trim() != "") {
        RemoteJoinIncomingCall joinCall = new RemoteJoinIncomingCall((ExecutionContext) this.getFramework()
            .getApplicationContext(), req);
        SipURI joineeURI = joinCall.getJoinee();
        Participant participant = this.getFramework().getApplicationContext().getParticipant(joineeURI.getUser());

        joinCall.addObserver(new Observer() {
          @State
          public void handleJoinComplete(JoinCompleteEvent event) {
            if (event.getCause() == Cause.BUSY) {
              LOG.warn("Join Policy violated when processing incoming remotejoin.");
              try {
                req.createResponse(SipServletResponse.SC_BUSY_HERE,
                    event.getException() != null ? event.getException().getMessage() : "").send();
              }
              catch (Exception ex) {
                LOG.warn("Exception when sending back remotejoin response.", ex);
              }
            }
          }
        });

        LOG.debug("Received remotejoin, joining. joiner:" + joinCall.getJoiner().getUser() + ". joinee:"
            + joinCall.getJoinee().getUser() + ". created RemoteJoinIncomingCall:" + joinCall);
        joinCall.join(participant, joinCall.getX_Join_Type(), joinCall.getX_Join_Force(),
            joinCall.getX_Join_Direction());
      }
      else {
        final IncomingCall ev = _app.getApplicationContext().getService(IncomingCallFactory.class)
            .createIncomingCall(req);

        _app.dispatch(ev);
      }
    }
    else {
      final EventSource source = SessionUtils.getEventSource(req);
      if (source != null && source instanceof Call) {
        source.dispatch(new SIPReInviteEventImpl((Call) source, req));
      }
      else{
        LOG.warn("Can't recognize event source for re-INVITE request:" + source + " request:" + req);
      }
    }
  }

  protected void doBye(final SipServletRequest req) throws ServletException, IOException {
    final EventSource source = SessionUtils.getEventSource(req);
    if (source != null && source instanceof SIPCall) {
      source.dispatch(new SIPHangupEventImpl((SIPCall) source, req));
    }
    else {
      LOG.warn("Can't recognize event source for BYE request:" + source + " request:" + req);
    }
  }

  protected void doAck(final SipServletRequest req) throws ServletException, IOException {
    final EventSource source = SessionUtils.getEventSource(req);
    if (source instanceof SIPCallImpl) {
      final SIPCallImpl call = (SIPCallImpl) source;
      try {
        call.doAck(req);
      }
      catch (final Exception e) {
        LOG.warn("", e);
      }
    }
    else{
      LOG.warn("Can't recognize event source for ACK request:" + source + " request:" + req);
    }
  }

  protected void doPrack(final SipServletRequest req) throws ServletException, IOException {
    final EventSource source = SessionUtils.getEventSource(req);
    if (source instanceof SIPIncomingCall) {
      final SIPIncomingCall call = (SIPIncomingCall) source;
      try {
        call.doPrack(req);
      }
      catch (final Exception e) {
        LOG.warn("", e);
      }
    }
  }

  protected void doCancel(final SipServletRequest req) throws ServletException, IOException {
    final EventSource source = SessionUtils.getEventSource(req);
    if (source instanceof SIPIncomingCall) {
      final SIPIncomingCall call = (SIPIncomingCall) source;
      try {
        call.doCancel(req);
      }
      catch (final Exception e) {
        LOG.warn("", e);
      }
    }

//     if (source != null) {
//       source.dispatch(new SIPCancelEventImpl((SIPCall) source, req));
//     }
//     else{
//       LOG.warn("Can't find event source for CANCEL request:" + req);
//       req.createResponse(200).send();
//     }
  }

  protected void doRefer(final SipServletRequest req) throws ServletException, IOException {
    EventSource source = null;
    if (!req.isInitial()) {
      source = SessionUtils.getEventSource(req);
    }
    if (source != null) {
      if (source instanceof SIPCall) {
        final CallEvent event = new SIPReferEventImpl((SIPCall) source, req);
        source.dispatch(event);
      }
      else {
        LOG.warn("SIP Refer is received on an unknown source: " + source);
      }
    }
    else {
      LOG.warn("SIP Refer is received as an initial message.");
    }
  }

  protected void doNotify(final SipServletRequest req) throws ServletException, IOException {
    EventSource source = null;
    if (!req.isInitial()) {
      source = SessionUtils.getEventSource(req);
    }
    if (source != null) {
      final NotifyEvent<EventSource> event = new SIPNotifyEventImpl<EventSource>(source, req);
      source.dispatch(event);
    }
    else {
      final NotifyEvent<EventSource> event = new SIPNotifyEventImpl<EventSource>(_app, req);
      _app.dispatch(event, new NoHandleHandler<EventSource>(event, req));
    }
  }

  protected void doMessage(final SipServletRequest req) throws ServletException, IOException {
    req.createResponse(200).send();
    final String type = req.getContentType();
    if (type != null && type.startsWith("text/")) {
      EventSource source = null;
      if (!req.isInitial()) {
        source = SessionUtils.getEventSource(req);
      }
      if (source != null) {
        final TextEvent<EventSource> event = new SIPTextEventImpl<EventSource>(source, req);
        source.dispatch(event);
      }
      else {
        final TextEvent<EventSource> event = new SIPTextEventImpl<EventSource>(_app, req);
        _app.dispatch(event, new SessionDisposer(req));
      }
    }
    else {
      try {
        req.getApplicationSession().invalidate();
      }
      catch (final Throwable t) {
        ;
      }
    }
  }

  protected void doRegister(final SipServletRequest req) throws ServletException, IOException {
    URI uri = req.getRequestURI();
    if (uri.isSipURI()) {
      SipURI suri = (SipURI) uri;
      if (suri.getUser() == null) {
        final RegisterEvent event = new SIPRegisterEventImpl(_app, req);
        _app.dispatch(event, new NoHandleHandler<Framework>(event, req));
        return;
      }
    }
    req.createResponse(SipServletResponse.SC_BAD_REQUEST, "Invalid Request URI").send();
  }

  protected void doSubscribe(final SipServletRequest req) throws ServletException, IOException {
    final SubscribeEvent event = new SIPSubscribeEventImpl(_app, req);
    _app.dispatch(event, new NoHandleHandler<Framework>(event, req));
  }

  protected void doUpdate(final SipServletRequest req) throws ServletException, IOException {
    final EventSource source = SessionUtils.getEventSource(req);
    if (source != null) {
      source.dispatch(new SIPUpdateEventImpl((Call) source, req));
    }
    else{
      LOG.warn("Can't find call for UPDATE message, discarding:" +req);
    }
  }

  protected void doOptions(final SipServletRequest req) throws ServletException, IOException {
    doOthers(req);
  }

  protected void doInfo(final SipServletRequest req) throws ServletException, IOException {
    req.createResponse(200).send();
    final String type = req.getContentType();
    if (type != null && type.equalsIgnoreCase("application/dtmf-relay") || type.equalsIgnoreCase("application/dtmf")) {
      EventSource source = SessionUtils.getEventSource(req);
      if (source != null && source instanceof Call) {
        final MohoInputDetectedEvent<Call> event = new MohoInputDetectedEvent<Call>((Call) source, getDTMFValue(req));
        source.dispatch(event);
      }
    }
  }

  private String getDTMFValue(SipServletRequest req) throws IOException {
    final String type = req.getContentType();
    String content = new String(req.getRawContent(), "UTF-8").trim();
    if (type.equalsIgnoreCase("application/dtmf-relay")) {
      Matcher matcher = DtmfRelayPattern.pattern.matcher(content);
      return matcher.group(1);
    }
    return content;
  }

  private static class DtmfRelayPattern {
    public static Pattern pattern = Pattern.compile("Signal\\s*=\\s*([\\d|A|B|C|D|\\*|#])");
  }

  protected void doPublish(final SipServletRequest req) throws ServletException, IOException {
    final PublishEvent event = new SIPPublishEventImpl(getFramework(), req);
    _app.dispatch(event, new NoHandleHandler<Framework>(event, req));
  }

  protected void doOthers(final SipServletRequest req) {
    EventSource source = null;
    if (!req.isInitial()) {
      source = SessionUtils.getEventSource(req);
    }
    if (source != null) {
      if (source instanceof Call) {
        final UnknownRequestEvent<Call> event = new SIPUnknownRequestEventImpl<Call>((Call) source, req);
        source.dispatch(event);
      }
      else if (source instanceof Framework) {
        final UnknownRequestEvent<Framework> event = new SIPUnknownRequestEventImpl<Framework>((Framework) source, req);
        source.dispatch(event);
      }
      else {
        final UnknownRequestEvent<EventSource> event = new SIPUnknownRequestEventImpl<EventSource>(source, req);
        source.dispatch(event);
      }
    }
    else {
      final UnknownRequestEvent<Framework> event = new SIPUnknownRequestEventImpl<Framework>((Framework) _app, req);
      _app.dispatch(event, new NoHandleHandler<Framework>(event, req));
    }
  }

  @Override
  public void doResponse(final SipServletResponse res) throws ServletException, IOException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Received response:" + res.toString().replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n"));
    }
    
    final EventSource source = SessionUtils.getEventSource(res);
    if (source != null) {
      final int i = res.getStatus();
      if (i < 200) {
        doProvisionalResponse(res);
      }
      else if (i < 300) {
        doSuccessResponse(res);
      }
      else if (res.isBranchResponse()) {
        doBranchResponse(res);
      }
      else if (i < 400) {
        doRedirectResponse(res);
      }
      else {
        doErrorResponse(res);
      }
    }
    else {
      final SipServletRequest req = (SipServletRequest) SIPHelper.getLinkSIPMessage(res.getRequest());
      if (req != null) {
        final SipServletResponse newRes = req.createResponse(res.getStatus(), res.getReasonPhrase());
        SIPHelper.copyContent(res, newRes);
        newRes.send();
      }
      else{
        LOG.warn(res + " can't find event source, and no linked sip message, discarding it.");
      }
    }
  }

  protected void doBranchResponse(final SipServletResponse res) throws ServletException, IOException {
    // do nothing right now
  }

  protected void doProvisionalResponse(final SipServletResponse res) throws ServletException, IOException {
    final EventSource source = SessionUtils.getEventSource(res);
    if (source != null) {
      if (source instanceof SIPCall) {
        source.dispatch(new SIPRingEventImpl((SIPCall) source, res));
        final int status = res.getStatus();
        if (SIPHelper.getRawContentWOException(res) != null && (SIPHelper.needPrack(res) || (Configuration.isEarlyMediaWithout100rel()) && !((SIPCallImpl)source).isDispatchedNon100relEarlyMedia())) {
          source.dispatch(new SIPEarlyMediaEventImpl((SIPCall) source, res));
        }
        else if (status != SipServletResponse.SC_TRYING) {
          if (source instanceof SIPCallImpl) {
            final SIPCallImpl call = (SIPCallImpl) source;
            try {
              call.doResponse(res, null);
            }
            catch (final Exception e) {
              LOG.warn("", e);
            }
          }
        }
      }
      else {
        LOG.warn(res + " is received for a non SIP Call source.");
      }
    }
    else{
      LOG.warn(res + " can't find event source: " + source);
    }
  }

  protected void doSuccessResponse(final SipServletResponse res) throws ServletException, IOException {
    final EventSource source = SessionUtils.getEventSource(res);
    if (source != null) {
      if (source instanceof SIPCallImpl) {
        final SIPCallImpl call = (SIPCallImpl) source;
        try {
          call.doResponse(res, null);
        }
        catch (final Exception e) {
          LOG.warn("", e);
        }
        if (SIPHelper.isInitial(res.getRequest())) {
          source.dispatch(new SIPAnsweredEventImpl<Call>((SIPCall) source, res));
        }
        return;
      }
      else if (source instanceof Framework) {
        source.dispatch(new SIPAnsweredEventImpl<Framework>((Framework) source, res));
        return;
      }
      else if (source instanceof Registration) {
        source.dispatch(new SIPAnsweredEventImpl<Registration>((Registration) source, res));
        return;
      }
      else if (source instanceof Subscription) {
        source.dispatch(new SIPAnsweredEventImpl<Subscription>((Subscription) source, res));
        return;
      }
      else{
        LOG.trace(res + " is received for a unknow source, dispatching: " + source);
        source.dispatch(new SIPAnsweredEventImpl(source, res));
      }
    }
    else{
      LOG.warn(res + " can't find event source: " + source);
    }
  }

  protected void doRedirectResponse(final SipServletResponse res) throws ServletException, IOException {
    final EventSource source = SessionUtils.getEventSource(res);
    if (source != null) {
      if (source instanceof SIPCall) {
        source.dispatch(new SIPRedirectEventImpl<Call>((SIPCall) source, res));
        return;
      }
      else if (source instanceof Framework) {
        source.dispatch(new SIPRedirectEventImpl<Framework>((Framework) source, res));
        return;
      }
      else if (source instanceof Registration) {
        source.dispatch(new SIPRedirectEventImpl<Registration>((Registration) source, res));
        return;
      }
      else if (source instanceof Subscription) {
        source.dispatch(new SIPRedirectEventImpl<Subscription>((Subscription) source, res));
        return;
      }
      else {
        LOG.trace(res + " is received for a unknow source, dispatching: " + source);
        source.dispatch(new SIPRedirectEventImpl( source, res));
      }
    }
    else{
      LOG.warn(res + " can't find event source: " + source);
    }
  }

  protected void doErrorResponse(final SipServletResponse res) throws ServletException, IOException {
    final EventSource source = SessionUtils.getEventSource(res);
    if(source != null){
      if (source instanceof SIPCallImpl) {
        final SIPCallImpl call = (SIPCallImpl) source;
        source.dispatch(new SIPDeniedEventImpl<Call>((SIPCall) source, res));
        try {
          call.doResponse(res, null);
        }
        catch (final Exception e) {
          LOG.warn("", e);
        }
      }
      else if (source instanceof Registration) {
        source.dispatch(new SIPDeniedEventImpl<Registration>((Registration) source, res));
      }
      else if (source instanceof Subscription) {
        source.dispatch(new SIPDeniedEventImpl<Subscription>((Subscription) source, res));
      }
      else {
        LOG.trace(res + " is received for a unknow source, dispatching: " + source);
        source.dispatch(new SIPDeniedEventImpl( source, res));
      }
    }
    else{
      LOG.warn(res + " can't find event source: " + source);
    }
  }

  private class NoHandleHandler<T extends EventSource> implements Runnable {

    private RequestEvent<T> _event;

    private SipServletRequest _req;

    private boolean _invalidate = false;

    public NoHandleHandler(final RequestEvent<T> event, final SipServletRequest req) {
      this(event, req, false);
    }

    public NoHandleHandler(final RequestEvent<T> event, final SipServletRequest req, final boolean invalidate) {
      _event = event;
      _req = req;
    }

    public void run() {
      if (!_event.isProcessed() && _req.isInitial() && !_req.isCommitted()) {
        try {
          _req.createResponse(SipServletResponse.SC_SERVER_INTERNAL_ERROR, "Request not handled by app").send();
        }
        catch (final Throwable t) {
          LOG.warn("", t);
        }
      }
      if (_invalidate) {
        try {
          _req.getApplicationSession().invalidate();
        }
        catch (final Throwable t) {
          LOG.warn("", t);
        }
      }
    }
  }

  private class SessionDisposer implements Runnable {

    private SipServletRequest _req;

    public SessionDisposer(final SipServletRequest req) {
      _req = req;
    }

    @Override
    public void run() {
      if (_req.isInitial()) {
        try {
          _req.getApplicationSession().invalidate();
        }
        catch (final Throwable t) {
          LOG.warn("", t);
        }
      }
    }

  }

  @Override
  public String getProtocolFamily() {
    return PROTOCOL_SIP;
  }

  @Override
  public String[] getEndpointSchemas() {
    return SCHEMAS;
  }

  @Override
  public SpiFramework getFramework() {
    return (SpiFramework) _app;
  }

  @Override
  public Endpoint createEndpoint(String addr) {
    try {
      if (addr.startsWith("sip:") || addr.startsWith("sips:") || addr.startsWith("tel:") || addr.startsWith("fax:")) {
        return new SIPEndpointImpl((ExecutionContext) _app, _sipFacory.createAddress(_sipFacory.createURI(addr)));
      }
      else {
        return new SIPEndpointImpl((ExecutionContext) _app, _sipFacory.createAddress(addr));
      }
    }
    catch (ServletParseException e) {
      LOG.error("", e);
      throw new IllegalArgumentException("not a legal sip address:" + addr, e);
    }
  }
}

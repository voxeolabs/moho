package com.voxeo.moho.sip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.networkconnection.SdpPortManagerEvent;
import javax.sdp.MediaDescription;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;
import javax.servlet.sip.Address;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;

import org.apache.log4j.Logger;

import com.voxeo.moho.SignalException;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.media.siprecord.metadata.Association;
import com.voxeo.moho.media.siprecord.metadata.CommunicationSession;
import com.voxeo.moho.media.siprecord.metadata.MediaStream;
import com.voxeo.moho.media.siprecord.metadata.ParticipantMetadata;
import com.voxeo.moho.media.siprecord.metadata.RecordingSession;
import com.voxeo.moho.spi.ExecutionContext;
import com.voxeo.moho.util.SDPUtils;

// TODO SRS failover?
public class SipRecordingCall extends SIPOutgoingCall {

  private static final Logger LOG = Logger.getLogger(SipRecordingCall.class);

  private String _label;

  private RecordingSession rsMetadata;

  private boolean pauseresumingRecording;

  private Object condition = new Object();

  private Exception asyncException;

  public SipRecordingCall(ExecutionContext context, SIPEndpoint from, SIPEndpoint to) {
    super(context, from, to, null);
  }

  public String getLabel() {
    return _label;
  }

  public void setLabel(String _label) {
    this._label = _label;
  }

  public RecordingSession getRSMetadata() {
    return rsMetadata;
  }

  public void setRSMetadata(RecordingSession rsMetadata) {
    this.rsMetadata = rsMetadata;
  }

  @Override
  public synchronized void onEvent(SdpPortManagerEvent event) {
    if (pauseresumingRecording) {
      if (event.isSuccessful()) {
        pauseRecordingComplete(null);
      }
      else {
        // pause recording failed
        pauseRecordingComplete(new SignalException("Failed process SDP."));
      }
    }
    else {
      super.onEvent(event);
    }
  }

  protected void pauseRecordingComplete(Exception ex) {
    if (ex != null) {
      LOG.error("Failed to pause/resume SIP recording", ex);
      asyncException = ex;
    }
    synchronized (condition) {
      pauseresumingRecording = false;
      condition.notifyAll();
    }
  }

  @Override
  protected synchronized void doResponse(SipServletResponse res, Map<String, String> headers) throws Exception {
    if (pauseresumingRecording) {
      if (res.getStatus() >= 200 && res.getStatus() < 300) {
        try {
          res.createAck().send();
          ((NetworkConnection) getMediaObject()).getSdpPortManager().processSdpOffer(res.getRawContent());
        }
        catch (Exception ex) {
          pauseRecordingComplete(ex);
        }
      }
      else if (res.getStatus() == SipServletResponse.SC_REQUEST_PENDING) {
        // handle 491 response
        super.doResponse(res, headers);
      }
      else {
        // pause recording failed
        pauseRecordingComplete(new SignalException("Re-Invite received error response " + res));
      }
    }
    else {
      super.doResponse(res, headers);
    }
  }

  @Override
  protected synchronized void call(byte[] sdp) throws IOException {
    if (isNoAnswered()) {
      if (_invite == null) {
        createRequest();
      }

      try {
        // add header
        _invite.addHeader("Require", "siprec");
        _invite.addHeader("MIME-Version", "1.0");
        Address contact = _invite.getAddressHeader("Contact");
        if (contact != null) {
          contact.setParameter("+sip.src", "");
        }

        if (sdp != null) {
          // modify the SDP, 1. modify sendrecv to sendonly 2. add the label
          // attribute
          SdpFactory sdpFactory = ((ExecutionContext) getApplicationContext()).getSdpFactory();
          SessionDescription sd = sdpFactory.createSessionDescription(new String(sdp, "iso8859-1"));

          MediaDescription md = ((MediaDescription) sd.getMediaDescriptions(false).get(0));
          md.removeAttribute("sendrecv");
          md.setAttribute("sendonly", null);
          // add label
          md.setAttribute("label", _label);
          setLocalSDP(sd.toString().getBytes("iso8859-1"));

          // create multipart content
          MimeMultipart multiPart = new MimeMultipart();

          MimeBodyPart sdpPart = new MimeBodyPart();
          sdpPart.setContent(SDPUtils.formulateSDP(this, sd), "application/sdp");
          sdpPart.addHeader("Content-Type", "application/sdp");
          multiPart.addBodyPart(sdpPart);

          MimeBodyPart metadataPart = new MimeBodyPart();
          metadataPart.setContent(rsMetadata.generateMetadataSnapshot().getBytes("iso8859-1"),
              "application/rs-metadata+xml");
          metadataPart.addHeader("Content-Type", "application/rs-metadata+xml");
          metadataPart.addHeader("Content-Disposition", "recording-session");
          multiPart.addBodyPart(metadataPart);

          ByteArrayOutputStream out = new ByteArrayOutputStream();
          multiPart.writeTo(out);
          _invite.setContent(out.toByteArray(), multiPart.getContentType());
        }
      }
      catch (Exception ex) {
        LOG.error("Exception when creating SIPRecording call.", ex);
        throw new SignalException(ex);
      }

      setSIPCallState(SIPCall.State.INVITING);
      _invite.send();
    }
    else if (isAnswered()) {
      reInviteRemote(sdp, null, null);
    }
  }

  protected void doDisconnect(final boolean failed, final CallCompleteEvent.Cause cause, final Exception exception,
      Map<String, String> headers, SIPCall.State old) {
    if (LOG.isDebugEnabled()) {
      LOG.debug(this + " is disconnecting.");
    }

    try {
      if (isNoAnswered(old)) {
        try {
          if (_invite != null
              && (_invite.getSession().getState() == SipSession.State.EARLY || _invite.getSession().getState() == SipSession.State.INITIAL)) {
            try {
              SipServletRequest cancelRequest = _invite.createCancel();
              SIPHelper.addHeaders(cancelRequest, headers);
              cancelRequest.send();
            }
            catch (Exception ex) {
              LOG.warn("Exception when disconnecting failed outbound call:" + ex.getMessage());
              _invite.getSession().invalidate();
            }
          }
        }
        catch (final Exception t) {
          LOG.warn("Exception when disconnecting call:" + t.getMessage());
        }
      }
      else if (isAnswered(old) && _invite.getSession().getState() != SipSession.State.TERMINATED) {
        try {
          // update metadata
          Map<CommunicationSession, Association> communicationSessions = rsMetadata.getCommunicationSessions();
          for (CommunicationSession communicationSession : communicationSessions.keySet()) {
            rsMetadata.disassotiateCommunicationSession(communicationSession);

            Map<ParticipantMetadata, Association> participants = communicationSession.getParticipants();
            for (ParticipantMetadata participant : participants.keySet()) {
              communicationSession.disassociateParticipant(participant);
            }

            Map<MediaStream, Association> mediaStreams = communicationSession.getMediaStreams();
            for (MediaStream mediaStream : mediaStreams.keySet()) {
              communicationSession.disassociateMediaStream(mediaStream);
            }
          }

          SipServletRequest byeReq = _signal.createRequest("BYE");
          SIPHelper.addHeaders(byeReq, headers);
          // add metadata
          byeReq.setContent(rsMetadata.generateMetadataSnapshot().getBytes("iso8859-1"), "application/rs-metadata+xml");
          byeReq.addHeader("Content-Disposition", "recording-session");
          byeReq.send();
        }
        catch (final Exception t) {
          LOG.warn("Exception when disconnecting call:" + t.getMessage());
        }
      }
    }
    finally {
      if (_invite != null) {
        SipApplicationSession appSession = _invite.getApplicationSession();

        try {
          if (appSession.isReadyToInvalidate()) {
            appSession.invalidate();
            if (LOG.isDebugEnabled()) {
              LOG.debug(appSession.getId() + " invalidated");
            }
          }
        }
        catch (IllegalStateException doofus) {
          try {
            appSession.invalidate();
            if (LOG.isDebugEnabled()) {
              LOG.debug(appSession.getId() + " invalidated anyway");
            }
          }
          catch (Exception ex) {
            LOG.warn("Exception caught while invalidating SipApplicationSession " + appSession.getId(), ex);
          }
        }
      }
    }

    terminate(cause, exception, null);
  }

  public void pauseRecording() {
    if (isTerminated()) {
      LOG.debug("SIPRecording call already terminated." + this);
      return;
    }
    pauseresumingRecording = true;

    try {
      SdpFactory sdpFactory = ((ExecutionContext) getApplicationContext()).getSdpFactory();
      SessionDescription sd = sdpFactory.createSessionDescription(new String(this.getLocalSDP(), "iso8859-1"));

      MediaDescription md = ((MediaDescription) sd.getMediaDescriptions(false).get(0));
      md.removeAttribute("sendrecv");
      md.removeAttribute("sendonly");
      md.setAttribute("inactive", null);
      md.setAttribute("label", _label);
      setLocalSDP(sd.toString().getBytes("iso8859-1"));

      reInviteRemote(sd.toString().getBytes("iso8859-1"), null, null);

      synchronized (condition) {
        long startTime = System.currentTimeMillis();
        while (pauseresumingRecording && System.currentTimeMillis() - startTime <= 40000) {
          condition.wait(40000);
        }
      }

      if (pauseresumingRecording) {
        LOG.error("Timeout when pausing SIPRecording " + this);
        throw new SignalException("Timeout when pausing SIPRecording " + this);
      }

      if (asyncException != null) {
        throw new SignalException(asyncException);
      }

      LOG.debug("Paused SIPRecording call:" + this);
    }
    catch (Exception ex) {
      LOG.error("Excetpion when pausing SIPRecording " + this, ex);
      throw new SignalException(ex);
    }
    finally {
      pauseresumingRecording = false;
    }
  }

  public void resumeRecording() {
    if (isTerminated()) {
      LOG.debug("SIPRecording call already terminated." + this);
      return;
    }
    // re-INVITE SRS, sends a new SDP offer and sets the media stream to
    // sendonly (a=sendonly)
    pauseresumingRecording = true;

    try {
      SdpFactory sdpFactory = ((ExecutionContext) getApplicationContext()).getSdpFactory();
      SessionDescription sd = sdpFactory.createSessionDescription(new String(this.getLocalSDP(), "iso8859-1"));

      MediaDescription md = ((MediaDescription) sd.getMediaDescriptions(false).get(0));
      md.removeAttribute("inactive");
      md.removeAttribute("sendrecv");
      md.setAttribute("sendonly", null);
      md.setAttribute("label", _label);
      setLocalSDP(sd.toString().getBytes("iso8859-1"));

      reInviteRemote(sd.toString().getBytes("iso8859-1"), null, null);

      synchronized (condition) {
        long startTime = System.currentTimeMillis();
        while (pauseresumingRecording && System.currentTimeMillis() - startTime <= 40000) {
          condition.wait(40000);
        }
      }

      if (pauseresumingRecording) {
        LOG.error("Timeout when resuming SIPRecording " + this);
        throw new SignalException("Timeout when resuming SIPRecording " + this);
      }

      if (asyncException != null) {
        throw new SignalException(asyncException);
      }

      LOG.debug("Resumed SIPRecording call:" + this);
    }
    catch (Exception ex) {
      LOG.error("Excetpion when resuming SIPRecording " + this, ex);
      throw new SignalException(ex);
    }
    finally {
      pauseresumingRecording = false;
    }
  }
}

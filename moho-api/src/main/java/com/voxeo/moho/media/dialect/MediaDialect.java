package com.voxeo.moho.media.dialect;

import java.net.URI;

import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.Value;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.resource.RTC;

import com.voxeo.moho.media.InputMode;

public interface MediaDialect {

  void setSpeechLanguage(Parameters parameters, String value);

  void setSpeechTermChar(Parameters parameters, Character value);

  void setSpeechInputMode(Parameters parameters, InputMode value);

  void setTextToSpeechVoice(Parameters parameters, String value);

  void setTextToSpeechLanguage(Parameters parameters, String value);

  void setBeepOnConferenceEnter(Parameters parameters, Boolean value);

  void setBeepOnConferenceExit(Parameters parameters, Boolean value);

  void setDtmfHotwordEnabled(Parameters parameters, Boolean value);

  void setDtmfTypeaheadEnabled(Parameters parameters, Boolean value);

  void setConfidence(Parameters parameters, float value);

  void setSpeechIncompleteTimeout(Parameters parameters, long peechIncompleteTimeout);

  void setSpeechCompleteTimeout(Parameters parameters, long peechCompleteTimeout);

  void setMixerName(Parameters params, String name);
  
  void setCallRecordFileFormat(Parameters params, Value value);
  
  void setCallRecordAudioCodec(Parameters params, Value value);
  
  void startCallRecord(NetworkConnection nc, URI recordURI, RTC[] rtc, Parameters optargs, CallRecordListener listener) throws MsControlException;
  
  void stopCallRecord(NetworkConnection nc);
}

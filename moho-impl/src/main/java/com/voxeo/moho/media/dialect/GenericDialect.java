package com.voxeo.moho.media.dialect;

import javax.media.mscontrol.Parameters;

import com.voxeo.moho.media.InputMode;

public class GenericDialect implements MediaDialect {

    @Override
    public void setBeepOnConferenceEnter(Parameters parameters, Boolean value) {}

    @Override
    public void setBeepOnConferenceExit(Parameters parameters, Boolean value) {}

    @Override
    public void setSpeechInputMode(Parameters parameters, InputMode value) {}

    @Override
    public void setSpeechLanguage(Parameters parameters, String value) {}

    @Override
    public void setSpeechTermChar(Parameters parameters, Character value) {}

    @Override
    public void setTextToSpeechVoice(Parameters parameters, String value) {}

    @Override
    public void setDtmfHotwordEnabled(Parameters parameters, Boolean value) {}

    @Override
    public void setDtmfTypeaheadEnabled(Parameters parameters, Boolean value) {}

}

package com.voxeo.moho.event;

import com.voxeo.moho.Participant;

public class ActiveSpeakerEvent extends MediaEvent {

    private Participant[] activeSpeakers;

    public ActiveSpeakerEvent(EventSource source, Participant[] activeSpeakers) {
        super(source);
        this.activeSpeakers = activeSpeakers;
    }

    public void setActiveSpeakers(Participant[] activeSpeakers) {
        this.activeSpeakers = activeSpeakers;
    }

    public Participant[] getActiveSpeakers() {
        return activeSpeakers;
    }

}

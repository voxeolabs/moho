package com.voxeo.moho.presence.sip;

import com.voxeo.moho.presence.SubscriptionState;

public interface SipSubscriptionState extends SubscriptionState {

 String getPhrase();
 
 String getReason();
 
 int getResponseCode();
}
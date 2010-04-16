package com.voxeo.moho.sample;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.media.mscontrol.join.Joinable;

import com.voxeo.moho.Application;
import com.voxeo.moho.ApplicationContext;
import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.State;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.event.InviteEvent;
import com.voxeo.moho.event.SignalEvent.Reason;
import com.voxeo.moho.media.output.OutputCommand;
import com.voxeo.moho.media.output.TextToSpeechResource;

/**
 * Black list(incoming call screening) example.
 */
public class BlackList implements Application {

  Map<String, List<String>> _blacklists = new HashMap<String, List<String>>();

  OutputCommand _prompt = new OutputCommand(new TextToSpeechResource("Please wait while we connect you."));

  @Override
  public void destroy() {
  }

  @Override
  public void init(final ApplicationContext ctx) {
    final String blacklist = ctx.getParameter("BlackList");
    if (blacklist != null) {
      try {
        final Properties p = new Properties();
        p.load(new FileInputStream(blacklist));
        final Enumeration<?> e = p.propertyNames();
        while (e.hasMoreElements()) {
          final String key = (String) e.nextElement();
          final String values = p.getProperty(key);
          if (values != null && values.length() > 0) {
            final List<String> list = new ArrayList<String>();
            for (final String value : values.split(",")) {
              list.add(value.trim());
            }
            _blacklists.put(key, list);
          }
        }
      }
      catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }
    System.out.println("BlackList: " + _blacklists);
  }

  @State
  public void handleInvite(final InviteEvent inv) throws Exception {
    final Endpoint caller = inv.getInvitor();
    final CallableEndpoint callee = inv.getInvitee();
    final List<String> blacklist = _blacklists.get(callee.getName());
    if (blacklist != null && blacklist.contains(caller.getName())) {
      inv.reject(Reason.FORBIDEN);
      return;
    }

    final Call call = inv.acceptCall();
    call.join().get();
    call.getMediaService().output(_prompt).get();
    call.join(callee, JoinType.DIRECT, Joinable.Direction.DUPLEX);
  }
}

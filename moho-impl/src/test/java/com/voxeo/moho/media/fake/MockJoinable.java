package com.voxeo.moho.media.fake;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.join.Joinable;

public class MockJoinable implements Joinable {

  private String id = UUID.randomUUID().toString();

  private List<Joinable> joinables;

  @Override
  public final Joinable[] getJoinees() throws MsControlException {
    if (joinables == null) {
      joinables = new LinkedList<Joinable>();
    }
    return joinables.toArray(new Joinable[] {});
  }

  @Override
  public final Joinable[] getJoinees(Direction arg0) throws MsControlException {
    if (joinables == null) {
      joinables = new LinkedList<Joinable>();
    }
    return joinables.toArray(new Joinable[] {});
  }

  @Override
  public final void join(Direction arg0, Joinable arg1) throws MsControlException {
    if (joinables == null) {
      joinables = new LinkedList<Joinable>();
    }
    joinables.add(arg1);

  }

  @Override
  public final void joinInitiate(Direction arg0, Joinable arg1, Serializable arg2) throws MsControlException {
    if (joinables == null) {
      joinables = new LinkedList<Joinable>();
    }
    joinables.add(arg1);
  }

  @Override
  public final void unjoin(Joinable arg0) throws MsControlException {
    if (joinables == null) {
      joinables = new LinkedList<Joinable>();
    }
    joinables.remove(arg0);
  }

  @Override
  public final void unjoinInitiate(Joinable arg0, Serializable arg1) throws MsControlException {
    if (joinables == null) {
      joinables = new LinkedList<Joinable>();
    }
    joinables.remove(arg0);
  }
}

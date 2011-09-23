package com.voxeo.moho.presence;

import java.io.Serializable;

public interface NotifyBody extends Serializable {
  String getName();

  String getContent();
}

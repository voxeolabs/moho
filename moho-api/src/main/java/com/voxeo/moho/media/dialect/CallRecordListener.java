package com.voxeo.moho.media.dialect;

import javax.media.mscontrol.resource.ResourceEvent;

public interface CallRecordListener {

  @SuppressWarnings("rawtypes")
  void callRecordComplete(ResourceEvent event);

}

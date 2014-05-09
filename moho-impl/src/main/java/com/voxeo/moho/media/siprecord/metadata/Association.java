package com.voxeo.moho.media.siprecord.metadata;

import java.util.Date;

public class Association {

  private Date _assotiateTimestamp;

  private Date _disassotiateTimestamp;

  public Association(Date assotiateTimestamp) {
    super();
    this._assotiateTimestamp = assotiateTimestamp;
  }

  public Date getAssotiateTimestamp() {
    return _assotiateTimestamp;
  }

  public void setAssotiateTimestamp(Date assotiateTimestamp) {
    this._assotiateTimestamp = assotiateTimestamp;
  }

  public Date getDisassotiateTimestamp() {
    return _disassotiateTimestamp;
  }

  public void setDisassotiateTimestamp(Date _disassotiateTimestamp) {
    this._disassotiateTimestamp = _disassotiateTimestamp;
  }

}

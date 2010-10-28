package com.voxeo.moho;

import java.io.IOException;

public interface TextableEndpoint extends Endpoint {

  void sendText(TextableEndpoint from, String text) throws IOException;

  void sendText(TextableEndpoint from, String text, String type) throws IOException;

}

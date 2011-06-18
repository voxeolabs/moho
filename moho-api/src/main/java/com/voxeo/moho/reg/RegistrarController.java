package com.voxeo.moho.reg;

import com.voxeo.moho.event.RegisterEvent.Contact;

public interface RegistrarController {
  void onExpire(Contact contact);
  void onRegister(Contact contact);
}

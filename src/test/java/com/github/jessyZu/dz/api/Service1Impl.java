package com.github.jessyZu.dz.api;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by zhoulai on 16/9/25.
 */
public class Service1Impl implements Service1 {
  private volatile boolean invoked = false;

  @Autowired
  Service2 service2;

  public void hi() {
    System.out.println("Service1.hi at " + new Date());
    service2.hi();
    service2.hello();
    invoked = true;
  }

  public boolean isInvoked() {
    return invoked;
  }
}

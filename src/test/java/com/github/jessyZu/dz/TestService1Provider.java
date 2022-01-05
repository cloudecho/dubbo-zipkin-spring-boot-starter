package com.github.jessyZu.dz;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.github.jessyZu.dz.api.Service1Impl;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = {"spring.application.name=provider1"})
@ContextConfiguration(locations = {"classpath:spring/dubbo-demo-service1-provider.xml"}, classes = TestApp.class)
public class TestService1Provider {
  @Autowired
  Service1Impl service1;

  @SuppressWarnings("BusyWait")
  @Test
  public void test() throws InterruptedException {
    while (!service1.isInvoked()) {
      Thread.sleep(1000);
    }
  }
}

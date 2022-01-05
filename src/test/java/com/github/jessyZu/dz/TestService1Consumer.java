package com.github.jessyZu.dz;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.github.jessyZu.dz.api.Service1;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = {"spring.application.name=consumer"})
@ContextConfiguration(locations = {"classpath:spring/dubbo-demo-service1-consumer.xml"}, classes = TestApp.class)
public class TestService1Consumer {

  @Autowired
  Service1 service1;

  @Test
  public void test() throws InterruptedException {
    boolean success = false;

    for (int i = 0; i < 5; i++) {
      if (i > 0) {
        Thread.sleep(2000);
      }
      try {
        service1.hi();
        //service1.hello();
        success = true;
        break;
      } catch (Exception e) {
        System.out.println(e);
      }
    }

    assertThat(success).isTrue();
  }

}

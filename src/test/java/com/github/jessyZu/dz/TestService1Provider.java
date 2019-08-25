package com.github.jessyZu.dz;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = {"spring.application.name=provider1"})
@ContextConfiguration(locations = {"classpath:spring/dubbo-demo-service1-provider.xml"}, classes = TestApp.class)
public class TestService1Provider {


    @Test
    public void test() {
        try {
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

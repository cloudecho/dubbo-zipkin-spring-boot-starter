package com.github.jessyZu.dz.api;

import java.util.Date;

/**
 * Created by zhoulai on 16/9/25.
 */
public class Service2Impl implements  Service2 {


    public void  hi(){
        System.out.println("Service2.hi at " + new Date());
    }
}

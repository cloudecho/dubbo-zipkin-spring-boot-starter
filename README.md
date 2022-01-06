# dubbo-zipkin-spring-boot-starter

Zipkin是一款开源的分布式实时数据追踪系统, `dubbo-zipkin-spring-boot-starter`是为服务治理框架dubbo 编写的instrument library,
支持dubbo全链路实时调用数据统计。使用者可以直接引入此boot starter，扩展默认自动激活(AutoConfiguration)。


## spring-boot 用户如何引入使用

```
    <dependency>
      <groupId>com.github.cloudecho</groupId>
      <artifactId>dubbo-zipkin-spring-boot-starter</artifactId>
      <version>1.1.1</version>
    </dependency>
```

默认会自动配置zikpin功能，也可以这样关闭：

```
	dubbo.trace.enabled=false
```

## 运行自带的测试

### 1.下载zipkin数据收集服务器包 
[zipkin-server-2.23.16-exec.jar](https://repo1.maven.org/maven2/io/zipkin/zipkin-server/2.23.16/zipkin-server-2.23.16-exec.jar),
这里以本地内存服务器做演示:

```sh
java -jar ./zipkin-server-2.23.16-exec.jar
```

打开 [http://localhost:9411/](http://localhost:9411/)  页面


### 2.按顺序分别运行Dubbo的服务调用与提供者

* 运行TestService2Provider.test()
* 运行TestService1Provider.test()
* 运行TestService1Consumer.test()


说明:

* 调用关系为TestService1Consumer->TestService1Provider->TestService2Provider;
* 一次调用会产生4个span，用来记录全链路的调用数据


### 3.调用成功后，打开 [http://localhost:9411/](http://localhost:9411/)  页面，查询调用数据

如图：

![zipkin-demo](./zipkin-demo.png)

## CHANGELOG

See [CHANGELOG](./CHANGELOG.md)

## Reference

* https://github.com/openzipkin/zipkin/

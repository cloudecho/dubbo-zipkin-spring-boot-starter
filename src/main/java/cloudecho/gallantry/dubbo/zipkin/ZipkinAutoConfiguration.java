package cloudecho.gallantry.dubbo.zipkin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import brave.Tracing;
import brave.context.slf4j.MDCScopeDecorator;
import brave.propagation.ThreadLocalCurrentTraceContext;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Sender;
import zipkin2.reporter.kafka.KafkaSender;

@Configuration
@ConditionalOnClass(ByteArraySerializer.class)
@EnableConfigurationProperties(KafkaProperties.class)
public class ZipkinAutoConfiguration {
  static final String SENDER_BEAN_NAME = "zipkinSender";

  @Value("${spring.zipkin.kafka.topic:zipkin}")
  private String topic;

  @Value("${spring.application.name:unknown}")
  private String applicationName;

  static String join(List<?> parts) {
    StringBuilder to = new StringBuilder();
    for (int i = 0, length = parts.size(); i < length; i++) {
      to.append(parts.get(i));
      if (i + 1 < length) {
        to.append(',');
      }
    }
    return to.toString();
  }

  @Bean(SENDER_BEAN_NAME)
  Sender zipkinSender(KafkaProperties config) {
    Map<String, Object> properties = config.buildProducerProperties();
    properties.put("key.serializer", ByteArraySerializer.class.getName());
    properties.put("value.serializer", ByteArraySerializer.class.getName());
    // Kafka expects the input to be a String, but KafkaProperties returns a list
    Object bootstrapServers = properties.get("bootstrap.servers");
    if (bootstrapServers instanceof List) {
      properties.put("bootstrap.servers", join((List) bootstrapServers));
    }
    return KafkaSender.newBuilder().topic(this.topic).overrides(properties)
        .build();
  }

  @Bean
  public Tracing tracing(@Autowired @Qualifier(SENDER_BEAN_NAME) Sender sender) {
    return Tracing.newBuilder()
        .currentTraceContext(ThreadLocalCurrentTraceContext.newBuilder()
            .addScopeDecorator(MDCScopeDecorator.create())
            .build())
        .spanReporter(AsyncReporter
            .builder(sender)
            .closeTimeout(1, TimeUnit.SECONDS)
            .build())
        .localServiceName(applicationName)
        .build();
  }
}

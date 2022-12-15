package com.hatef.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kafka.producer")
@Getter
@Setter
public class KafkaProducerAppProperties {
    private String topic;
    private String messageKey;
    private String brokerAddress;
    private String zookeeperConnect;
    private int retries;
    private String keySerializer;
    private String valueSerializer;
}

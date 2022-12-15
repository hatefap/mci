package com.hatef.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kafka.consumer")
@Getter
@Setter
public class KafkaConsumerAppProperties {
    private String topic;
    private String messageKey;
    private String brokerAddress;
    private String zookeeperConnect;
    private int retries;
    private String keyDeserializer;
    private String valueDeserializer;
    private String consumerGroup;
}

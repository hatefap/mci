package com.hatef.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppConfigProperties {
    private int readPollTime;
    private int maxMessagePerPoll;
    private int threadPoolCoreSize;
    private String threadPoolPrefix;
}

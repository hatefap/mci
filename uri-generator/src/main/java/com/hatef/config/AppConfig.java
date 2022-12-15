package com.hatef.config;

import com.hatef.service.SimpleUrlGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.kafka.outbound.KafkaProducerMessageHandler;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;

@Configuration
@EnableConfigurationProperties(value =
        {AppConfigProperties.class, KafkaProducerAppProperties.class})
@Slf4j
public class AppConfig {

    private final AppConfigProperties appConfigProperties;
    private final KafkaProducerAppProperties kafkaProducerAppProperties;

    public AppConfig(AppConfigProperties appConfigProperties,
                     KafkaProducerAppProperties kafkaProducerAppProperties) {
        this.appConfigProperties = appConfigProperties;
        this.kafkaProducerAppProperties = kafkaProducerAppProperties;
    }

    @Bean
    public QueueChannel urlChannel() {
        return MessageChannels.queue(Integer.MAX_VALUE).get();
    }

    @Bean
    public MessageSource<String> generateUrlIntoChannel() {
        var generator = new SimpleUrlGenerator(1_000_000);
        return () -> {
            Optional<String> url = generator.generate();
            return url.map(GenericMessage::new).orElse(null);
        };
    }

    @Bean
    @ServiceActivator(inputChannel = "urlChannel")
    public MessageHandler handler() {
        KafkaProducerMessageHandler<String, String> handler =
                new KafkaProducerMessageHandler<>(kafkaTemplate());
        handler.setTopicExpression(new LiteralExpression(kafkaProducerAppProperties.getTopic()));
        handler.setMessageKeyExpression(
                new FunctionExpression<Message<String>>(m ->
                        Objects.requireNonNull(m.getHeaders().get(kafkaProducerAppProperties.getMessageKey())).toString()));
        return handler;
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProducerAppProperties.getBrokerAddress());
        props.put(ProducerConfig.RETRIES_CONFIG, kafkaProducerAppProperties.getRetries());
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, kafkaProducerAppProperties.getKeySerializer());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, kafkaProducerAppProperties.getValueSerializer());
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public NewTopic topic(KafkaProducerAppProperties properties) {
        return new NewTopic(properties.getTopic(), 1, (short) 1);
    }

    @Bean
    public IntegrationFlow putIntoKafkaFlow() {
        return IntegrationFlow
                // The following queue is the repo we are going to put URLs
                .from(generateUrlIntoChannel(), configurer -> configurer.poller(
                        pollerConfigurer -> pollerConfigurer.fixedDelay(1000)
                                .taskExecutor(Executors.newSingleThreadScheduledExecutor())
                                .maxMessagesPerPoll(1000)))
                .channel(urlChannel())
                // We are going to read from repo and store it in kafka
                .handle(handler(), configurer -> configurer.poller(
                        pollerConfigurer -> pollerConfigurer.fixedDelay(appConfigProperties.getReadPollTime())
                                .taskExecutor(Executors.newSingleThreadScheduledExecutor())
                                .maxMessagesPerPoll(appConfigProperties.getMaxMessagePerPoll())))
                .get();
    }
}

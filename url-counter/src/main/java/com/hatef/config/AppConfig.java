package com.hatef.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.google.common.primitives.Longs;
import com.hatef.handler.WindowCountMessageHandler;
import com.hatef.model.EsUrlDataModel;
import com.hatef.output.ElasticSearchRepositoryImpl;
import com.hatef.output.RedisUrlOutputPort;
import com.hatef.output.UrlOutputPort;
import com.hatef.output.UrlRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.kafka.dsl.Kafka;
import org.springframework.integration.kafka.dsl.KafkaMessageDrivenChannelAdapterSpec;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(value =
        {AppConfigProperties.class, KafkaConsumerAppProperties.class})
public class AppConfig {
    private final KafkaConsumerAppProperties kafkaConsumerAppProperties;
    private final AppConfigProperties appConfigProperties;

    public AppConfig(KafkaConsumerAppProperties kafkaConsumerAppProperties,
                     AppConfigProperties appConfigProperties) {
        this.kafkaConsumerAppProperties = kafkaConsumerAppProperties;
        this.appConfigProperties = appConfigProperties;
    }


    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConsumerAppProperties.getBrokerAddress());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, kafkaConsumerAppProperties.getKeyDeserializer());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, kafkaConsumerAppProperties.getValueDeserializer());
        props.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, false);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 100);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 15000);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaConsumerAppProperties.getConsumerGroup());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public QueueChannel fromKafka() {
        return MessageChannels.queue(Integer.MAX_VALUE).get();
    }


    @Bean
    public IntegrationFlow readFromKafka() {
        KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec<String, String> messageListenerSpec
                = Kafka.messageDrivenChannelAdapter(consumerFactory(), kafkaConsumerAppProperties.getTopic());
        messageListenerSpec.outputChannel(fromKafka());
        return IntegrationFlow
                .from(messageListenerSpec)
                .get();
    }

    @Bean
    public WindowCountMessageHandler getElasticSearchMessageHandler(
            @Qualifier(value = "esChannel") PollableChannel outputChannel, UrlOutputPort urlOutputPort) {
        return new WindowCountMessageHandler(appConfigProperties.getMinSeenToStore(),
                appConfigProperties.getWindowSizeMinute(), outputChannel, urlOutputPort);
    }

    @Bean
    public IntegrationFlow prepareForElasticSearchFlow(WindowCountMessageHandler esHandler) {
        return IntegrationFlow
                .from(fromKafka())
                .handle(esHandler)
                .get();
    }

    @Bean
    public QueueChannel esChannel() {
        return MessageChannels.queue(Integer.MAX_VALUE).get();
    }
    @Bean
    public UrlRepository getUrlRepository(ElasticsearchClient esClient){
        return new ElasticSearchRepositoryImpl(esClient, appConfigProperties.getEsIndexName());
    }

    @Bean
    @ServiceActivator(inputChannel = "esChannel")
    public MessageHandler insertIntoElasticsearchOutputAdapter(UrlRepository repository) {
        return message -> {
            var payload = (EsUrlDataModel) message.getPayload();
            repository.createIndex(payload);
        };
    }

    @Bean
    public IntegrationFlow storeIntoElasticSearch(UrlRepository repository) {
        return IntegrationFlow
                .from(esChannel())
                .handle(insertIntoElasticsearchOutputAdapter(repository))
                .get();
    }

    @Bean
    public UrlOutputPort urlOutputPort(RedisTemplate<String, Long> redisTemplate){
        return new RedisUrlOutputPort(redisTemplate, appConfigProperties.getRedisScanBatchSize());
    }

    @Bean
    public RedisTemplate<String, Long> redisTemplate() {
        var redisConfiguration = new RedisStandaloneConfiguration();
        redisConfiguration.setHostName(appConfigProperties.getRedisHost());
        redisConfiguration.setPort(appConfigProperties.getRedisPort());
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory(redisConfiguration);
        jedisConnectionFactory.afterPropertiesSet();
        RedisTemplate<String, Long> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(jedisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new LongSerializer());
        redisTemplate.setEnableTransactionSupport(true);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    static class LongSerializer implements RedisSerializer<Long>{

        @Override
        public byte[] serialize(Long aLong) throws SerializationException {
            return Longs.toByteArray(aLong);
        }

        @Override
        public Long deserialize(byte[] bytes) throws SerializationException {
            return Longs.fromByteArray(bytes);
        }
    }
    @Configuration
    static class ElasticsearchClientConfig extends ElasticsearchConfiguration {
        private final String ipAndPort;

        public ElasticsearchClientConfig(@Value("${app.esIpAndPort}") String ipAndPort) {
            this.ipAndPort = ipAndPort;
        }

        @Override
        public ClientConfiguration clientConfiguration() {
            return ClientConfiguration
                    .builder()
                    .connectedTo(ipAndPort)
                    .build();
        }
    }
}

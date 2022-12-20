package com.hatef.handler;

import com.hatef.model.EsUrlDataModel;
import com.hatef.output.UrlOutputPort;

import java.time.Instant;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;

public class WindowCountMessageHandler extends AbstractMessageHandler {
    private final int minSeenToStore;
    private final PollableChannel outputChannel;
    private final UrlOutputPort urlOutputPort;

    public WindowCountMessageHandler(int minSeenToStore,
                                     int windowSize,
                                     PollableChannel outputChannel,
                                     UrlOutputPort urlOutputPort) {
        this.minSeenToStore = minSeenToStore;
        this.outputChannel = outputChannel;
        this.urlOutputPort = urlOutputPort;
        var ses = Executors.newSingleThreadScheduledExecutor();
        var redisEntryConsumer = getRedisEntryConsumer();

        ses.scheduleWithFixedDelay(() -> {
            try (var redisCursor = urlOutputPort.findAllUrl()) {
                redisCursor
                        .stream()
                        .parallel()
                        .forEach(redisEntryConsumer);
            }
        }, 0, windowSize, TimeUnit.MINUTES);
    }

    private Consumer<Entry<String, Long>> getRedisEntryConsumer() {
        return entry -> {
            if (entry.getValue() < minSeenToStore) {
                urlOutputPort.deleteUrl(entry.getKey());
                return;
            }
            System.out.println(Thread.currentThread().getName() + " " + entry);
            EsUrlDataModel payload = EsUrlDataModel.builder()
                    .url(entry.getKey())
                    .timestamp(Instant.now().toEpochMilli())
                    .numberOfSeen(entry.getValue())
                    .build();
            if (this.outputChannel.send(new GenericMessage<>(payload)))
                urlOutputPort.deleteUrl(entry.getKey());
        };
    }

    @Override
    protected void handleMessageInternal(Message<?> message) {
        var url = (String) message.getPayload();
        urlOutputPort.incrementUrl(url);
    }
}

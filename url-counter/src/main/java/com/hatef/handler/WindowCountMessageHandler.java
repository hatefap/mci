package com.hatef.handler;

import com.hatef.model.EsUrlDataModel;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WindowCountMessageHandler extends AbstractMessageHandler {
    private final Map<String, Integer> urlCount;
    private final int minSeenToStore;
    private final PollableChannel outputChannel;

    public WindowCountMessageHandler(int minSeenToStore, int windowSize, PollableChannel outputChannel) {
        this.minSeenToStore = minSeenToStore;
        this.outputChannel = outputChannel;
        this.urlCount = new ConcurrentHashMap<>(512);
        var ses = Executors.newSingleThreadScheduledExecutor();
        ses.scheduleWithFixedDelay(() -> {
            for (var entry : urlCount.entrySet()){
                if(entry.getValue() >= this.minSeenToStore){
                    EsUrlDataModel payload = EsUrlDataModel.builder()
                            .url(entry.getKey())
                            .timestamp(Instant.now().toEpochMilli())
                            .numberOfSeen(entry.getValue())
                            .build();
                    if(this.outputChannel.send(new GenericMessage<>(payload)))
                        urlCount.remove(entry.getKey());
                }
                else
                    urlCount.remove(entry.getKey());
            }
        }, 0, windowSize, TimeUnit.MINUTES);
    }

    @Override
    protected void handleMessageInternal(Message<?> message) {
        var m = (String) message.getPayload();
        urlCount.put(m, urlCount.getOrDefault(m, 0) + 1);
    }
}

package com.hatef.output;

import java.util.Map;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

public class RedisUrlOutputPort implements UrlOutputPort {

    // https://www.techgeeknext.com/spring-boot/spring-boot-redis-example
    public static final String HASH_KEY_NAME = "URL-ITEM";
    private final HashOperations<String, String, Long> hashOps;
    private final int redisScanBatchSize;

    public RedisUrlOutputPort(RedisTemplate<String, Long> redisTemplate, int redisScanBatchSize) {
        this.hashOps = redisTemplate.opsForHash();
        this.redisScanBatchSize = redisScanBatchSize;
    }

    @Override
    public Long incrementUrl(String url) {
        /*
         *   Here is why we didn't use hashOps.increment
         *   https://stackoverflow.com/questions/74847936/spring-boot-throws-eofexception-when-reading-from-redis
         * */
        var currentVal = hashOps.get(HASH_KEY_NAME, url);
        var newVal = currentVal == null ? 1 : currentVal + 1;
        hashOps.put(HASH_KEY_NAME, url, currentVal == null ? 1 : currentVal + 1);
        return newVal;
    }

    @Override
    public Cursor<Map.Entry<String, Long>> findAllUrl() {
        return hashOps.scan(HASH_KEY_NAME, ScanOptions.scanOptions().count(redisScanBatchSize).build());
    }

    @Override
    public Long getUrl(String url) {
        return hashOps.get(HASH_KEY_NAME, url);
    }

    public void deleteUrl(String url) {
        hashOps.delete(HASH_KEY_NAME, url);
    }

    @Override
    public void deleteAll() {
        hashOps.delete(HASH_KEY_NAME);
    }
}

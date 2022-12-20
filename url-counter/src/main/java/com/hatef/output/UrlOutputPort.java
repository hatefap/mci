package com.hatef.output;

import org.springframework.data.redis.core.Cursor;

import java.util.Map;

public interface UrlOutputPort {
    Long incrementUrl(String url);
    Cursor<Map.Entry<String, Long>> findAllUrl();
    Long getUrl(String url);
    void deleteUrl(String url);
    void deleteAll();
}

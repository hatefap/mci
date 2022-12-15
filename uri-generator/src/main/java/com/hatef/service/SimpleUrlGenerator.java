package com.hatef.service;

import com.namics.commons.random.RandomData;
import lombok.SneakyThrows;

import java.net.URL;
import java.util.*;

/* not thread safe */
public class SimpleUrlGenerator implements UrlGenerator{
    private final int limit;
    private int counter;

    public SimpleUrlGenerator(int limit) {
        if (limit <= 0)
            throw new IllegalArgumentException("limit must be greater than zero.");
        this.limit = limit;
    }


    @SneakyThrows
    @Override
    public Optional<String> generate() {
        if(counter < limit)
            return Optional.of(new URL("https", RandomData.lastname().toLowerCase() + "."
                    + RandomData.countryCode().toLowerCase(),"/test").toString());
        return Optional.empty();
    }

    public int getLimit() {
        return limit;
    }

    public int getCounter() {
        return counter;
    }

    public void resetGenerator(){
        counter = 0;
    }
}

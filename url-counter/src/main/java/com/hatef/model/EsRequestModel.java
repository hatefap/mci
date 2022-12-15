package com.hatef.model;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EsRequestModel {
    private Integer from;
    private Integer size;

    public EsRequestModel(Integer from, Integer size) {
        setFrom(from);
        setSize(size);
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        if(from < 0){
            log.info("'from' is less than zero reset it to 0");
            this.from = 0;
            return;
        }
        this.from = from;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        if(size > 100){
            log.info("'size' passed the maximum allowed value which is 100, reset it to 100");
            this.size = 100;
            return;
        } else if(size <= 0){
            log.info("reset size to default value which is 10");
            this.size = 10;
            return;
        }
        this.size = size;
    }
}

package com.hatef.model;

import lombok.*;
import org.checkerframework.checker.index.qual.NegativeIndexFor;

import java.time.Instant;

@Builder
@ToString
@EqualsAndHashCode
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EsUrlDataModel {
    private String url;
    private long timestamp;
    private int numberOfSeen;
}

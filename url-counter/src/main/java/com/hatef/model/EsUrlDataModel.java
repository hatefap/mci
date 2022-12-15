package com.hatef.model;

import lombok.*;

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

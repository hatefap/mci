package com.hatef.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EsUrlDataModel {
    private String url;
    private long timestamp;
    private long numberOfSeen;
}

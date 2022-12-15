package com.hatef.output;

import com.hatef.model.EsRequestModel;
import com.hatef.model.EsUrlDataModel;

import java.util.List;

public interface UrlRepository {
    boolean createIndex(EsUrlDataModel doc);
    List<EsUrlDataModel> search(EsRequestModel request);
}

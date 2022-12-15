package com.hatef.output;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import com.hatef.model.EsRequestModel;
import com.hatef.model.EsUrlDataModel;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

@Slf4j
public class ElasticSearchRepositoryImpl implements UrlRepository {

    private final ElasticsearchClient esClient;
    private final String index;

    public ElasticSearchRepositoryImpl(ElasticsearchClient esClient, String index) {
        this.esClient = esClient;
        this.index = index;
    }

    @Override
    public boolean createIndex(EsUrlDataModel doc) {
        IndexRequest<EsUrlDataModel> request = IndexRequest.of(
                builder -> builder.index(index).document(doc));
        try{
            esClient.index(request);
            return true;
        } catch (IOException ex){
            log.error("Error while saving doc: " + doc + " to ElasticSearch server.", ex);
        }
        return false;
    }

    @Override
    public List<EsUrlDataModel> search(EsRequestModel request) {
        return null;
    }
}

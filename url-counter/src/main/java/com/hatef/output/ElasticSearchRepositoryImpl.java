package com.hatef.output;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.hatef.model.EsRequestModel;
import com.hatef.model.EsUrlDataModel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Collections;
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
        var request = IndexRequest.of(
                builder -> builder.index(index).document(doc));
        try {
            esClient.index(request);
            return true;
        } catch (IOException ex) {
            log.error("Error while saving doc: " + doc + " to ElasticSearch server.", ex);
        }
        return false;
    }

    @SneakyThrows
    @Override
    public List<EsUrlDataModel> search(EsRequestModel request) {
        var req = SearchRequest.of(builder ->
                builder
                        .from(request.getFrom())
                        .size(request.getSize())
                        .index(index));
        var searchResults = esClient
                .search(req, EsUrlDataModel.class)
                .hits().hits();
        return searchResults.stream().map(Hit::source).toList();
    }

    @SneakyThrows
    @Override
    public List<EsUrlDataModel> topKVisit(int k) {
        if (k <= 0)
            return Collections.emptyList();
        var sort = new SortOptions
                .Builder()
                .field(f -> f.field("numberOfSeen").order(SortOrder.Desc)).build();
        var request = SearchRequest.of(builder ->
                builder
                        .index(index)
                        .size(k)
                        .sort(sort));
        var searchResults = esClient
                .search(request, EsUrlDataModel.class)
                .hits().hits();
        return searchResults.stream().map(Hit::source).toList();
    }
}

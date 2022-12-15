package com.hatef.controller;


import com.hatef.model.EsRequestModel;
import com.hatef.model.EsUrlDataModel;
import com.hatef.output.UrlRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping(value = "/api/v1")
@RestController
public class UrlController {

    private final UrlRepository repository;

    public UrlController(UrlRepository repository) {
        this.repository = repository;
    }

    @GetMapping(value = "urls")
    public List<EsUrlDataModel> urls(@RequestParam(defaultValue = "0") int from, @RequestParam(defaultValue = "10") int size) {
        EsRequestModel request = new EsRequestModel(from, size);
        return repository.search(request);
    }

    @GetMapping(value = "topk")
    public List<EsUrlDataModel> urls(@RequestParam(defaultValue = "10") int k) {
        return repository.topKVisit(k);
    }


}

package com.hatef.controller;


import com.hatef.output.UrlRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping(value = "/api/v1")
@RestController
public class UrlController {

    private final UrlRepository repository;
    public UrlController(UrlRepository repository) {
        this.repository = repository;
    }


}

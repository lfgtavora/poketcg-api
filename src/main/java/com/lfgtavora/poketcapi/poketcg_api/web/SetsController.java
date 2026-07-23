package com.lfgtavora.poketcapi.poketcg_api.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.lfgtavora.poketcapi.poketcg_api.service.SetReadService;
import com.lfgtavora.poketcapi.poketcg_api.web.dto.PagedApiResponse;
import com.lfgtavora.poketcapi.poketcg_api.web.dto.SingleApiResponse;

@RestController
@RequestMapping("/v2/sets")
public class SetsController {

    private final SetReadService setReadService;

    public SetsController(SetReadService setReadService) {
        this.setReadService = setReadService;
    }

    @GetMapping
    public PagedApiResponse<JsonNode> search(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "250") int pageSize,
            @RequestParam(name = "orderBy", required = false) String orderBy) {
        return setReadService.searchSets(q, page, pageSize, orderBy);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SingleApiResponse<JsonNode>> getById(@PathVariable("id") String id) {
        return setReadService.findById(id)
                .map(set -> ResponseEntity.ok(new SingleApiResponse<>(set)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

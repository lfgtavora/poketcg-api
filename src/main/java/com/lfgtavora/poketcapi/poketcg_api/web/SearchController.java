package com.lfgtavora.poketcapi.poketcg_api.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lfgtavora.poketcapi.poketcg_api.service.AutocompleteSearchService;
import com.lfgtavora.poketcapi.poketcg_api.web.dto.AutocompleteItem;
import com.lfgtavora.poketcapi.poketcg_api.web.dto.SingleApiResponse;

@RestController
@RequestMapping("/v2/search")
public class SearchController {

    private final AutocompleteSearchService autocompleteSearchService;

    public SearchController(AutocompleteSearchService autocompleteSearchService) {
        this.autocompleteSearchService = autocompleteSearchService;
    }

    @GetMapping
    public SingleApiResponse<List<AutocompleteItem>> search(
            @RequestParam(name = "query") String query,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "types", required = false) String types) {
        return autocompleteSearchService.search(query, limit, types);
    }
}

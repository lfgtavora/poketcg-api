package com.lfgtavora.poketcapi.poketcg_api.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.lfgtavora.poketcapi.poketcg_api.service.CardReadService;
import com.lfgtavora.poketcapi.poketcg_api.web.dto.PagedApiResponse;
import com.lfgtavora.poketcapi.poketcg_api.web.dto.SingleApiResponse;

@RestController
@RequestMapping("/v2/cards")
public class CardsController {

    private final CardReadService cardReadService;

    public CardsController(CardReadService cardReadService) {
        this.cardReadService = cardReadService;
    }

    @GetMapping
    public PagedApiResponse<JsonNode> search(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "250") int pageSize,
            @RequestParam(name = "orderBy", required = false) String orderBy,
            @RequestParam(name = "select", required = false) String select) {
        return cardReadService.searchCards(q, page, pageSize, orderBy, select);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SingleApiResponse<JsonNode>> getById(
            @PathVariable("id") String id,
            @RequestParam(name = "select", required = false) String select) {
        return cardReadService.findById(id, select)
                .map(card -> ResponseEntity.ok(new SingleApiResponse<>(card)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

package com.lfgtavora.poketcapi.poketcg_api.service;

public record SyncRunResult(
        boolean changed,
        String revision,
        long cardsProcessed,
        long setsProcessed,
        String status,
        String message) {
}

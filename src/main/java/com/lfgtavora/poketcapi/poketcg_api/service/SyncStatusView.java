package com.lfgtavora.poketcapi.poketcg_api.service;

import java.time.Instant;

public record SyncStatusView(
        String revision,
        Instant lastSyncAt,
        String status,
        String lastError) {
}

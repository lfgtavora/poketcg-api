package com.lfgtavora.poketcapi.poketcg_api.sync;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import com.lfgtavora.poketcapi.poketcg_api.service.DataSyncService;
import com.lfgtavora.poketcapi.poketcg_api.service.SyncStatusView;

@Component
public class SyncHealthIndicator implements HealthIndicator {

    private final DataSyncService dataSyncService;

    public SyncHealthIndicator(DataSyncService dataSyncService) {
        this.dataSyncService = dataSyncService;
    }

    @Override
    public Health health() {
        SyncStatusView status = dataSyncService.getStatus();
        Health.Builder builder = "FAILED".equalsIgnoreCase(status.status()) ? Health.down() : Health.up();
        return builder
                .withDetail("syncStatus", status.status())
                .withDetail("revision", status.revision())
                .withDetail("lastSyncAt", status.lastSyncAt())
                .withDetail("lastError", status.lastError())
                .build();
    }
}

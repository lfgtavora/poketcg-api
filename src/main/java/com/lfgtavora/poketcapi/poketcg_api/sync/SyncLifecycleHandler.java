package com.lfgtavora.poketcapi.poketcg_api.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.lfgtavora.poketcapi.poketcg_api.service.DataSyncService;

@Component
public class SyncLifecycleHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncLifecycleHandler.class);

    private final DataSyncService dataSyncService;

    public SyncLifecycleHandler(DataSyncService dataSyncService) {
        this.dataSyncService = dataSyncService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartupSync() {
        runSyncSafely("startup", false);
    }

    @Scheduled(cron = "${poketcg.sync.cron}")
    public void scheduledSync() {
        runSyncSafely("scheduled", false);
    }

    private void runSyncSafely(String trigger, boolean force) {
        try {
            dataSyncService.syncIfNeeded(force);
        } catch (Exception exception) {
            LOGGER.error("Dataset sync failed for trigger={}", trigger, exception);
        }
    }
}

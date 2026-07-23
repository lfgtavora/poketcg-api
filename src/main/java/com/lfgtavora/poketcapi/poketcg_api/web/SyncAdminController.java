package com.lfgtavora.poketcapi.poketcg_api.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lfgtavora.poketcapi.poketcg_api.service.DataSyncService;
import com.lfgtavora.poketcapi.poketcg_api.service.SyncRunResult;
import com.lfgtavora.poketcapi.poketcg_api.service.SyncStatusView;

@RestController
@RequestMapping("/internal/sync")
public class SyncAdminController {

    private final DataSyncService dataSyncService;

    public SyncAdminController(DataSyncService dataSyncService) {
        this.dataSyncService = dataSyncService;
    }

    @PostMapping("/run")
    public SyncRunResult runSync(@RequestParam(name = "force", defaultValue = "false") boolean force) {
        return dataSyncService.syncIfNeeded(force);
    }

    @GetMapping("/status")
    public SyncStatusView status() {
        return dataSyncService.getStatus();
    }
}

package com.lfgtavora.poketcapi.poketcg_api.service;

import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.lfgtavora.poketcapi.poketcg_api.config.PokemonDataProperties;
import com.lfgtavora.poketcapi.poketcg_api.domain.SyncStateEntity;
import com.lfgtavora.poketcapi.poketcg_api.repository.SyncStateRepository;

@Service
public class DataSyncService {

    private static final long SYNC_STATE_ID = 1L;

    private final DatasetClient datasetClient;
    private final DatasetImportService datasetImportService;
    private final SyncStateRepository syncStateRepository;
    private final PokemonDataProperties properties;
    private final AutocompleteSearchService autocompleteSearchService;

    public DataSyncService(
            DatasetClient datasetClient,
            DatasetImportService datasetImportService,
            SyncStateRepository syncStateRepository,
            PokemonDataProperties properties,
            AutocompleteSearchService autocompleteSearchService) {
        this.datasetClient = datasetClient;
        this.datasetImportService = datasetImportService;
        this.syncStateRepository = syncStateRepository;
        this.properties = properties;
        this.autocompleteSearchService = autocompleteSearchService;
    }

    public SyncRunResult syncIfNeeded(boolean force) {
        if (!properties.getSync().isEnabled() && !force) {
            return new SyncRunResult(false, null, 0, 0, "DISABLED", "Synchronization is disabled");
        }

        String latestRevision = datasetClient.fetchLatestRevision();
        SyncStateEntity state = getOrCreateState();
        if (!force && latestRevision.equals(state.getLastRemoteRevision())) {
            return new SyncRunResult(false, latestRevision, 0, 0, "UNCHANGED", "Dataset revision unchanged");
        }

        markRunning(state);
        try (InputStream datasetStream = datasetClient.downloadDatasetSnapshot()) {
            DatasetImportService.ImportCounters counters = datasetImportService.replaceDataset(datasetStream);
            autocompleteSearchService.rebuildIndex();
            markSuccess(state, latestRevision);
            return new SyncRunResult(
                    true,
                    latestRevision,
                    counters.cards(),
                    counters.sets(),
                    "UPDATED",
                    "Dataset synchronized");
        } catch (Exception exception) {
            markFailure(state, exception.getMessage());
            throw new IllegalStateException("Failed to synchronize dataset", exception);
        }
    }

    public SyncStatusView getStatus() {
        SyncStateEntity state = getOrCreateState();
        return new SyncStatusView(
                state.getLastRemoteRevision(),
                state.getLastSyncAt(),
                state.getStatus(),
                state.getLastError());
    }

    private SyncStateEntity getOrCreateState() {
        Optional<SyncStateEntity> existing = syncStateRepository.findById(SYNC_STATE_ID);
        if (existing.isPresent()) {
            return existing.get();
        }

        SyncStateEntity state = new SyncStateEntity();
        state.setId(SYNC_STATE_ID);
        state.setStatus("IDLE");
        return syncStateRepository.save(state);
    }

    private void markRunning(SyncStateEntity state) {
        state.setStatus("RUNNING");
        state.setLastError(null);
        syncStateRepository.save(state);
    }

    private void markSuccess(SyncStateEntity state, String revision) {
        state.setStatus("SUCCESS");
        state.setLastError(null);
        state.setLastRemoteRevision(revision);
        state.setLastSyncAt(Instant.now());
        syncStateRepository.save(state);
    }

    private void markFailure(SyncStateEntity state, String errorMessage) {
        state.setStatus("FAILED");
        state.setLastError(errorMessage == null ? "Unknown sync error" : errorMessage);
        state.setLastSyncAt(Instant.now());
        syncStateRepository.save(state);
    }
}

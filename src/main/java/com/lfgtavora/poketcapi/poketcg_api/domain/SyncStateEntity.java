package com.lfgtavora.poketcapi.poketcg_api.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sync_state")
public class SyncStateEntity {

    @Id
    private Long id;

    @Column(name = "last_remote_revision", length = 80)
    private String lastRemoteRevision;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    @Column(name = "status", length = 32)
    private String status;

    @Column(name = "last_error", columnDefinition = "CLOB")
    private String lastError;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLastRemoteRevision() {
        return lastRemoteRevision;
    }

    public void setLastRemoteRevision(String lastRemoteRevision) {
        this.lastRemoteRevision = lastRemoteRevision;
    }

    public Instant getLastSyncAt() {
        return lastSyncAt;
    }

    public void setLastSyncAt(Instant lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
}

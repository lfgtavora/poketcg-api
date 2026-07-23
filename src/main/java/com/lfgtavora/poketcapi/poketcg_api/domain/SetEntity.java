package com.lfgtavora.poketcapi.poketcg_api.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "sets", indexes = {
        @Index(name = "idx_sets_name", columnList = "name"),
        @Index(name = "idx_sets_series", columnList = "series")
})
public class SetEntity {

    @Id
    @Column(length = 64, nullable = false, updatable = false)
    private String id;

    @Column(length = 255, nullable = false)
    private String name;

    @Column(length = 255)
    private String series;

    @Column(name = "printed_total")
    private Integer printedTotal;

    @Column(name = "total")
    private Integer total;

    @Column(name = "ptcgo_code", length = 32)
    private String ptcgoCode;

    @Column(name = "release_date", length = 64)
    private String releaseDate;

    @Column(name = "updated_at", length = 64)
    private String updatedAt;

    @Column(name = "raw_json", columnDefinition = "CLOB", nullable = false)
    private String rawJson;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public Integer getPrintedTotal() {
        return printedTotal;
    }

    public void setPrintedTotal(Integer printedTotal) {
        this.printedTotal = printedTotal;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public String getPtcgoCode() {
        return ptcgoCode;
    }

    public void setPtcgoCode(String ptcgoCode) {
        this.ptcgoCode = ptcgoCode;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getRawJson() {
        return rawJson;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }

    public Instant getSyncedAt() {
        return syncedAt;
    }

    public void setSyncedAt(Instant syncedAt) {
        this.syncedAt = syncedAt;
    }
}

package com.lfgtavora.poketcapi.poketcg_api.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "cards", indexes = {
        @Index(name = "idx_cards_name", columnList = "name"),
        @Index(name = "idx_cards_set_id", columnList = "set_id"),
        @Index(name = "idx_cards_supertype", columnList = "supertype"),
        @Index(name = "idx_cards_number", columnList = "card_number"),
        @Index(name = "idx_cards_set_id_number", columnList = "set_id, card_number"),
        @Index(name = "idx_cards_rarity", columnList = "rarity")
})
public class CardEntity {

    @Id
    @Column(length = 64, nullable = false, updatable = false)
    private String id;

    @Column(length = 255, nullable = false)
    private String name;

    @Column(name = "set_id", length = 64)
    private String setId;

    @Column(length = 128)
    private String supertype;

    @Column(columnDefinition = "TEXT")
    private String subtypes;

    @Column(columnDefinition = "TEXT")
    private String types;

    @Column(name = "card_number", length = 64)
    private String number;

    @Column(length = 255)
    private String artist;

    @Column(length = 128)
    private String rarity;

    @Column(length = 32)
    private String hp;

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

    public String getSetId() {
        return setId;
    }

    public void setSetId(String setId) {
        this.setId = setId;
    }

    public String getSupertype() {
        return supertype;
    }

    public void setSupertype(String supertype) {
        this.supertype = supertype;
    }

    public String getSubtypes() {
        return subtypes;
    }

    public void setSubtypes(String subtypes) {
        this.subtypes = subtypes;
    }

    public String getTypes() {
        return types;
    }

    public void setTypes(String types) {
        this.types = types;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public String getHp() {
        return hp;
    }

    public void setHp(String hp) {
        this.hp = hp;
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

package com.lfgtavora.poketcapi.poketcg_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lfgtavora.poketcapi.poketcg_api.domain.SyncStateEntity;

public interface SyncStateRepository extends JpaRepository<SyncStateEntity, Long> {
}

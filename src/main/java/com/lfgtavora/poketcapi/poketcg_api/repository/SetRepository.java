package com.lfgtavora.poketcapi.poketcg_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.lfgtavora.poketcapi.poketcg_api.domain.SetEntity;

public interface SetRepository extends JpaRepository<SetEntity, String>, JpaSpecificationExecutor<SetEntity> {

    @Query("select s.id from SetEntity s")
    List<String> findAllIds();
}

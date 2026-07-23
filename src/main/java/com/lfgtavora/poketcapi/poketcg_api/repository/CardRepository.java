package com.lfgtavora.poketcapi.poketcg_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.lfgtavora.poketcapi.poketcg_api.domain.CardEntity;

public interface CardRepository extends JpaRepository<CardEntity, String>, JpaSpecificationExecutor<CardEntity> {

    @Query("select c.id from CardEntity c")
    List<String> findAllIds();
}

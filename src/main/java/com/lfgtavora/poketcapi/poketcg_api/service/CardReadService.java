package com.lfgtavora.poketcapi.poketcg_api.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lfgtavora.poketcapi.poketcg_api.domain.CardEntity;
import com.lfgtavora.poketcapi.poketcg_api.repository.CardRepository;
import com.lfgtavora.poketcapi.poketcg_api.web.dto.PagedApiResponse;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@Service
public class CardReadService {

    private static final int MAX_PAGE_SIZE = 250;

    private final CardRepository cardRepository;
    private final ObjectMapper objectMapper;

    public CardReadService(CardRepository cardRepository, ObjectMapper objectMapper) {
        this.cardRepository = cardRepository;
        this.objectMapper = objectMapper;
    }

    public PagedApiResponse<JsonNode> searchCards(String q, int page, int pageSize, String orderBy, String select) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        PageRequest request = PageRequest.of(safePage - 1, safePageSize, buildSort(orderBy));
        Specification<CardEntity> spec = buildSpecification(q);
        Page<CardEntity> result = cardRepository.findAll(spec, request);

        List<JsonNode> data = result.getContent().stream()
                .map(card -> FieldSelector.apply(readRawJson(card), select, objectMapper))
                .toList();
        return new PagedApiResponse<>(data, safePage, safePageSize, data.size(), result.getTotalElements());
    }

    public Optional<JsonNode> findById(String id, String select) {
        return cardRepository.findById(id)
                .map(card -> FieldSelector.apply(readRawJson(card), select, objectMapper));
    }

    private JsonNode readRawJson(CardEntity cardEntity) {
        try {
            return objectMapper.readTree(cardEntity.getRawJson());
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to deserialize card payload: " + cardEntity.getId(), exception);
        }
    }

    private Sort buildSort(String orderBy) {
        if (orderBy == null || orderBy.isBlank()) {
            return Sort.by(Sort.Order.asc("setId"), Sort.Order.asc("number"));
        }

        String[] tokens = orderBy.split(",");
        Sort sort = Sort.unsorted();
        for (String token : tokens) {
            String raw = token.trim();
            if (raw.isEmpty()) {
                continue;
            }

            Sort.Direction direction = Sort.Direction.ASC;
            if (raw.startsWith("-")) {
                direction = Sort.Direction.DESC;
                raw = raw.substring(1);
            }

            String field = switch (raw) {
                case "id", "name", "supertype", "updatedAt", "rarity", "artist", "hp", "number" -> raw;
                case "set.id" -> "setId";
                default -> throw new IllegalArgumentException("Unsupported orderBy field: " + raw);
            };
            sort = sort.and(Sort.by(new Sort.Order(direction, field)));
        }

        if (sort.isUnsorted()) {
            return Sort.by(Sort.Order.asc("setId"), Sort.Order.asc("number"));
        }

        // Keep set-relative number order stable when only one field is requested.
        boolean hasSetId = sort.stream().anyMatch(order -> "setId".equals(order.getProperty()));
        boolean hasNumber = sort.stream().anyMatch(order -> "number".equals(order.getProperty()));
        if (hasNumber && !hasSetId) {
            sort = Sort.by(Sort.Order.asc("setId")).and(sort);
        } else if (hasSetId && !hasNumber) {
            sort = sort.and(Sort.by(Sort.Order.asc("number")));
        }
        return sort;
    }

    private Specification<CardEntity> buildSpecification(String query) {
        Optional<QueryParser.Expression> expression = QueryParser.parse(query, "name");
        if (expression.isEmpty()) {
            return null;
        }

        return (root, ignored, criteriaBuilder) -> {
            return toPredicate(expression.get(), root, criteriaBuilder);
        };
    }

    private Predicate toPredicate(QueryParser.Expression expression, Root<CardEntity> root, CriteriaBuilder cb) {
        if (expression instanceof QueryParser.Term term) {
            return toTermPredicate(term, root, cb);
        }
        if (expression instanceof QueryParser.And andExpr) {
            return cb.and(toPredicate(andExpr.left(), root, cb), toPredicate(andExpr.right(), root, cb));
        }
        if (expression instanceof QueryParser.Or orExpr) {
            return cb.or(toPredicate(orExpr.left(), root, cb), toPredicate(orExpr.right(), root, cb));
        }
        if (expression instanceof QueryParser.Not notExpr) {
            return cb.not(toPredicate(notExpr.expression(), root, cb));
        }
        throw new IllegalArgumentException("Unsupported query expression");
    }

    private Predicate toTermPredicate(QueryParser.Term term, Root<CardEntity> root, CriteriaBuilder cb) {
        String value = term.value();
        String lowered = value.toLowerCase(Locale.ROOT);
        return switch (term.field()) {
            case "id" -> value.contains("*")
                    ? cb.like(cb.lower(root.get("id")), wildcardToLike(lowered))
                    : cb.equal(root.get("id"), value);
            case "name" -> cb.like(cb.lower(root.get("name")), wildcardOrContains(lowered));
            case "set.id" -> value.contains("*")
                    ? cb.like(cb.lower(root.get("setId")), wildcardToLike(lowered))
                    : cb.equal(root.get("setId"), value);
            case "supertype" -> value.contains("*")
                    ? cb.like(cb.lower(root.get("supertype")), wildcardToLike(lowered))
                    : cb.equal(cb.lower(root.get("supertype")), lowered);
            case "types" -> cb.like(cb.lower(root.get("types")), wildcardOrContains(lowered));
            case "subtypes" -> cb.like(cb.lower(root.get("subtypes")), wildcardOrContains(lowered));
            case "number" -> value.contains("*")
                    ? cb.like(cb.lower(root.get("number")), wildcardToLike(lowered))
                    : cb.equal(root.get("number"), value);
            case "rarity" -> cb.like(cb.lower(root.get("rarity")), wildcardOrContains(lowered));
            case "artist" -> cb.like(cb.lower(root.get("artist")), wildcardOrContains(lowered));
            case "hp" -> value.contains("*")
                    ? cb.like(cb.lower(root.get("hp")), wildcardToLike(lowered))
                    : cb.equal(root.get("hp"), value);
            default -> throw new IllegalArgumentException("Unsupported card query field: " + term.field());
        };
    }

    private String wildcardOrContains(String lowered) {
        return lowered.contains("*") ? wildcardToLike(lowered) : "%" + lowered + "%";
    }

    private String wildcardToLike(String value) {
        return value.replace("*", "%");
    }
}

package com.lfgtavora.poketcapi.poketcg_api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

final class FieldSelector {

    private FieldSelector() {
    }

    static JsonNode apply(JsonNode source, String select, ObjectMapper objectMapper) {
        if (select == null || select.isBlank() || source == null || !source.isObject()) {
            return source;
        }

        ObjectNode filtered = objectMapper.createObjectNode();
        for (String token : select.split(",")) {
            String field = token.trim();
            if (field.isEmpty()) {
                continue;
            }
            if (source.has(field)) {
                filtered.set(field, source.get(field));
            }
        }
        return filtered;
    }
}

package com.lfgtavora.poketcapi.poketcg_api.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AutocompleteItem(
        String type,
        String id,
        String name,
        SetRef set,
        String series,
        Images images) {

    public record SetRef(String id, String name) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Images(String small, String large, String symbol, String logo) {
        public static Images forCard(String small, String large) {
            return new Images(small, large, null, null);
        }

        public static Images forSet(String symbol, String logo) {
            return new Images(null, null, symbol, logo);
        }
    }

    public static AutocompleteItem card(String id, String name, String setId, String setName, Images images) {
        return new AutocompleteItem("card", id, name, new SetRef(setId, setName), null, images);
    }

    public static AutocompleteItem set(String id, String name, String series, Images images) {
        return new AutocompleteItem("set", id, name, null, series, images);
    }
}

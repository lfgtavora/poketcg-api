package com.lfgtavora.poketcapi.poketcg_api.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lfgtavora.poketcapi.poketcg_api.domain.CardEntity;
import com.lfgtavora.poketcapi.poketcg_api.domain.SetEntity;
import com.lfgtavora.poketcapi.poketcg_api.repository.CardRepository;
import com.lfgtavora.poketcapi.poketcg_api.repository.SetRepository;
import com.lfgtavora.poketcapi.poketcg_api.web.dto.AutocompleteItem;
import com.lfgtavora.poketcapi.poketcg_api.web.dto.AutocompleteItem.Images;
import com.lfgtavora.poketcapi.poketcg_api.web.dto.SingleApiResponse;

import jakarta.annotation.PostConstruct;

@Service
public class AutocompleteSearchService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;
    private static final int MIN_FUZZY_QUERY_LENGTH = 3;
    private static final Set<SearchType> ALL_TYPES = EnumSet.allOf(SearchType.class);

    private final CardRepository cardRepository;
    private final SetRepository setRepository;
    private final ObjectMapper objectMapper;
    private final AtomicReference<List<IndexedItem>> index = new AtomicReference<>(List.of());

    public AutocompleteSearchService(
            CardRepository cardRepository,
            SetRepository setRepository,
            ObjectMapper objectMapper) {
        this.cardRepository = cardRepository;
        this.setRepository = setRepository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void rebuildIndex() {
        List<SetEntity> sets = setRepository.findAll();
        Map<String, SetEntity> setsById = sets.stream()
                .collect(Collectors.toMap(SetEntity::getId, Function.identity(), (left, right) -> left));

        List<IndexedItem> items = new ArrayList<>(sets.size() + 16_384);
        for (SetEntity set : sets) {
            items.add(IndexedItem.set(set, extractImages(set.getRawJson(), SearchType.SET)));
        }
        for (CardEntity card : cardRepository.findAll()) {
            SetEntity set = setsById.get(card.getSetId());
            String setName = set != null ? set.getName() : null;
            items.add(IndexedItem.card(card, setName, extractImages(card.getRawJson(), SearchType.CARD)));
        }

        index.set(List.copyOf(items));
    }

    public SingleApiResponse<List<AutocompleteItem>> search(String query, Integer limit, String types) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("query must not be blank");
        }

        Set<SearchType> allowedTypes = parseTypes(types);
        int safeLimit = limit == null ? DEFAULT_LIMIT : Math.min(Math.max(limit, 1), MAX_LIMIT);
        String queryLower = trimmed.toLowerCase(Locale.ROOT);
        int maxEdits = maxEditsFor(queryLower.length());
        LevenshteinDistance distance = new LevenshteinDistance(maxEdits);

        List<ScoredItem> scored = new ArrayList<>();
        for (IndexedItem item : index.get()) {
            if (!allowedTypes.contains(item.searchType())) {
                continue;
            }
            Integer rank = rank(item.nameLower(), queryLower, distance, maxEdits);
            if (rank != null) {
                scored.add(new ScoredItem(item, rank));
            }
        }

        scored.sort(Comparator
                .comparingInt(ScoredItem::rank)
                .thenComparing(entry -> entry.item().nameLower())
                .thenComparing(entry -> entry.item().type())
                .thenComparing(entry -> entry.item().id()));

        List<AutocompleteItem> results = scored.stream()
                .limit(safeLimit)
                .map(entry -> entry.item().toAutocompleteItem())
                .toList();

        return new SingleApiResponse<>(results);
    }

    private Images extractImages(String rawJson, SearchType kind) {
        if (rawJson == null || rawJson.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode images = root.get("images");
            if (images == null || !images.isObject()) {
                return null;
            }
            return switch (kind) {
                case CARD -> {
                    String small = textOrNull(images.get("small"));
                    String large = textOrNull(images.get("large"));
                    yield (small == null && large == null) ? null : Images.forCard(small, large);
                }
                case SET -> {
                    String symbol = textOrNull(images.get("symbol"));
                    String logo = textOrNull(images.get("logo"));
                    yield (symbol == null && logo == null) ? null : Images.forSet(symbol, logo);
                }
            };
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isNull() || !node.isTextual()) {
            return null;
        }
        String value = node.asText();
        return value.isBlank() ? null : value;
    }

    private static Set<SearchType> parseTypes(String types) {
        if (types == null || types.isBlank()) {
            return ALL_TYPES;
        }

        EnumSet<SearchType> parsed = EnumSet.noneOf(SearchType.class);
        for (String part : types.split(",")) {
            String token = part.trim().toLowerCase(Locale.ROOT);
            if (token.isEmpty()) {
                continue;
            }
            SearchType searchType = SearchType.fromToken(token);
            if (searchType == null) {
                throw new IllegalArgumentException("types must be card, set, or both (comma-separated)");
            }
            parsed.add(searchType);
        }

        if (parsed.isEmpty()) {
            return ALL_TYPES;
        }
        return parsed;
    }

    private static int maxEditsFor(int queryLength) {
        if (queryLength < MIN_FUZZY_QUERY_LENGTH) {
            return 0;
        }
        return queryLength <= 4 ? 1 : 2;
    }

    private static Integer rank(String nameLower, String queryLower, LevenshteinDistance distance, int maxEdits) {
        if (nameLower.equals(queryLower)) {
            return 0;
        }
        if (nameLower.startsWith(queryLower)) {
            return 1;
        }
        if (nameLower.contains(queryLower)) {
            return 2;
        }
        if (maxEdits <= 0) {
            return null;
        }

        Integer fullDistance = distance.apply(queryLower, nameLower);
        if (isWithinThreshold(fullDistance)) {
            return 10 + fullDistance;
        }

        if (nameLower.length() > queryLower.length()) {
            int window = Math.min(nameLower.length(), queryLower.length() + maxEdits);
            Integer prefixDistance = distance.apply(queryLower, nameLower.substring(0, window));
            if (isWithinThreshold(prefixDistance)) {
                return 20 + prefixDistance;
            }
        }

        return null;
    }

    private static boolean isWithinThreshold(Integer distance) {
        return distance != null && distance >= 0;
    }

    private enum SearchType {
        CARD("card"),
        SET("set");

        private final String token;

        SearchType(String token) {
            this.token = token;
        }

        static SearchType fromToken(String token) {
            for (SearchType value : values()) {
                if (value.token.equals(token)) {
                    return value;
                }
            }
            return null;
        }
    }

    private record ScoredItem(IndexedItem item, int rank) {
    }

    private record IndexedItem(
            SearchType searchType,
            String type,
            String id,
            String name,
            String nameLower,
            String setId,
            String setName,
            String series,
            Images images) {

        static IndexedItem set(SetEntity set, Images images) {
            String name = Objects.requireNonNullElse(set.getName(), "");
            return new IndexedItem(
                    SearchType.SET,
                    "set",
                    set.getId(),
                    name,
                    name.toLowerCase(Locale.ROOT),
                    null,
                    null,
                    set.getSeries(),
                    images);
        }

        static IndexedItem card(CardEntity card, String setName, Images images) {
            String name = Objects.requireNonNullElse(card.getName(), "");
            return new IndexedItem(
                    SearchType.CARD,
                    "card",
                    card.getId(),
                    name,
                    name.toLowerCase(Locale.ROOT),
                    card.getSetId(),
                    setName,
                    null,
                    images);
        }

        AutocompleteItem toAutocompleteItem() {
            if (searchType == SearchType.SET) {
                return AutocompleteItem.set(id, name, series, images);
            }
            return AutocompleteItem.card(id, name, setId, setName, images);
        }
    }
}

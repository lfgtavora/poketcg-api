package com.lfgtavora.poketcapi.poketcg_api.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lfgtavora.poketcapi.poketcg_api.domain.CardEntity;
import com.lfgtavora.poketcapi.poketcg_api.domain.SetEntity;
import com.lfgtavora.poketcapi.poketcg_api.repository.CardRepository;
import com.lfgtavora.poketcapi.poketcg_api.repository.SetRepository;

@Service
public class DatasetImportService {

    private static final int BATCH_SIZE = 500;

    private final CardRepository cardRepository;
    private final SetRepository setRepository;
    private final ObjectMapper objectMapper;

    public DatasetImportService(CardRepository cardRepository, SetRepository setRepository, ObjectMapper objectMapper) {
        this.cardRepository = cardRepository;
        this.setRepository = setRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ImportCounters replaceDataset(InputStream datasetInputStream) throws IOException {
        AtomicLong cardsCounter = new AtomicLong(0);
        AtomicLong setsCounter = new AtomicLong(0);
        Instant syncedAt = Instant.now();
        Set<String> seenCardIds = new HashSet<>();
        Set<String> seenSetIds = new HashSet<>();

        List<CardEntity> cardsBuffer = new ArrayList<>(BATCH_SIZE);
        List<SetEntity> setsBuffer = new ArrayList<>(BATCH_SIZE);

        try (GzipCompressorInputStream gzipInputStream = new GzipCompressorInputStream(datasetInputStream);
                TarArchiveInputStream tarInputStream = new TarArchiveInputStream(gzipInputStream)) {

            TarArchiveEntry entry;
            while ((entry = tarInputStream.getNextEntry()) != null) {
                if (!entry.isFile() || !entry.getName().endsWith(".json")) {
                    continue;
                }

                String entryName = entry.getName();
                byte[] payload = tarInputStream.readAllBytes();
                JsonNode root = objectMapper.readTree(payload);

                if (isSetEntry(entryName)) {
                    for (JsonNode json : iterableRecords(root)) {
                        SetEntity setEntity = mapSet(json, syncedAt);
                        if (setEntity != null) {
                            seenSetIds.add(setEntity.getId());
                            setsBuffer.add(setEntity);
                            setsCounter.incrementAndGet();
                            if (setsBuffer.size() >= BATCH_SIZE) {
                                setRepository.saveAll(setsBuffer);
                                setsBuffer.clear();
                            }
                        }
                    }
                } else if (isCardEntry(entryName)) {
                    String fallbackSetId = extractSetIdFromEntry(entryName);
                    for (JsonNode json : iterableRecords(root)) {
                        CardEntity cardEntity = mapCard(json, syncedAt, fallbackSetId);
                        if (cardEntity != null) {
                            seenCardIds.add(cardEntity.getId());
                            cardsBuffer.add(cardEntity);
                            cardsCounter.incrementAndGet();
                            if (cardsBuffer.size() >= BATCH_SIZE) {
                                cardRepository.saveAll(cardsBuffer);
                                cardsBuffer.clear();
                            }
                        }
                    }
                }
            }
        }

        if (!setsBuffer.isEmpty()) {
            setRepository.saveAll(setsBuffer);
        }
        if (!cardsBuffer.isEmpty()) {
            cardRepository.saveAll(cardsBuffer);
        }

        deleteMissingCards(seenCardIds);
        deleteMissingSets(seenSetIds);

        return new ImportCounters(cardsCounter.get(), setsCounter.get());
    }

    private void deleteMissingCards(Set<String> seenCardIds) {
        List<String> staleIds = cardRepository.findAllIds().stream()
                .filter(existingId -> !seenCardIds.contains(existingId))
                .toList();
        deleteInChunks(staleIds, ids -> cardRepository.deleteAllByIdInBatch(ids));
    }

    private void deleteMissingSets(Set<String> seenSetIds) {
        List<String> staleIds = setRepository.findAllIds().stream()
                .filter(existingId -> !seenSetIds.contains(existingId))
                .toList();
        deleteInChunks(staleIds, ids -> setRepository.deleteAllByIdInBatch(ids));
    }

    private void deleteInChunks(List<String> ids, java.util.function.Consumer<List<String>> deleteFn) {
        if (ids.isEmpty()) {
            return;
        }
        for (int start = 0; start < ids.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, ids.size());
            deleteFn.accept(ids.subList(start, end));
        }
    }

    private boolean isCardEntry(String path) {
        String normalized = path.toLowerCase(Locale.ROOT);
        return normalized.contains("cards/en/")
                && !normalized.contains("cards/en/v1/")
                && normalized.endsWith(".json");
    }

    private boolean isSetEntry(String path) {
        String normalized = path.toLowerCase(Locale.ROOT);
        return normalized.contains("sets/")
                && !normalized.contains("decks/")
                && normalized.endsWith(".json");
    }

    private String extractSetIdFromEntry(String entryName) {
        int lastSlash = entryName.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash + 1 >= entryName.length()) {
            return null;
        }
        String fileName = entryName.substring(lastSlash + 1);
        if (!fileName.endsWith(".json")) {
            return null;
        }
        return fileName.substring(0, fileName.length() - ".json".length());
    }

    private CardEntity mapCard(JsonNode node, Instant syncedAt, String fallbackSetId) throws IOException {
        if (!hasRequiredFields(node, "id", "name")) {
            return null;
        }
        CardEntity card = new CardEntity();
        card.setId(requiredText(node, "id"));
        card.setName(requiredText(node, "name"));
        String setId = optionalText(node.path("set"), "id");
        if (setId == null || setId.isBlank()) {
            setId = fallbackSetId;
        }
        card.setSetId(setId);
        card.setSupertype(optionalText(node, "supertype"));
        card.setSubtypes(joinArray(node.path("subtypes")));
        card.setTypes(joinArray(node.path("types")));
        card.setNumber(optionalText(node, "number"));
        card.setArtist(optionalText(node, "artist"));
        card.setRarity(optionalText(node, "rarity"));
        card.setHp(optionalText(node, "hp"));
        card.setUpdatedAt(optionalText(node, "updatedAt"));
        card.setRawJson(objectMapper.writeValueAsString(withSetIdIfMissing(node, setId)));
        card.setSyncedAt(syncedAt);
        return card;
    }

    private SetEntity mapSet(JsonNode node, Instant syncedAt) throws IOException {
        if (!hasRequiredFields(node, "id", "name")) {
            return null;
        }
        SetEntity set = new SetEntity();
        set.setId(requiredText(node, "id"));
        set.setName(requiredText(node, "name"));
        set.setSeries(optionalText(node, "series"));
        set.setPrintedTotal(optionalInt(node, "printedTotal"));
        set.setTotal(optionalInt(node, "total"));
        set.setPtcgoCode(optionalText(node, "ptcgoCode"));
        set.setReleaseDate(optionalText(node, "releaseDate"));
        set.setUpdatedAt(optionalText(node, "updatedAt"));
        set.setRawJson(objectMapper.writeValueAsString(node));
        set.setSyncedAt(syncedAt);
        return set;
    }

    private String requiredText(JsonNode node, String field) {
        String value = optionalText(node, field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return value;
    }

    private List<JsonNode> iterableRecords(JsonNode root) {
        if (root == null || root.isNull()) {
            return List.of();
        }
        if (root.isArray()) {
            List<JsonNode> rows = new ArrayList<>();
            for (JsonNode item : root) {
                if (item != null && item.isObject()) {
                    rows.add(item);
                }
            }
            return rows;
        }
        if (root.isObject()) {
            return List.of(root);
        }
        return List.of();
    }

    private boolean hasRequiredFields(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode fieldNode = node.get(field);
            if (fieldNode == null || fieldNode.isNull() || fieldNode.asText().isBlank()) {
                return false;
            }
        }
        return true;
    }

    private JsonNode withSetIdIfMissing(JsonNode node, String setId) {
        if (!(node instanceof ObjectNode objectNode) || setId == null || setId.isBlank()) {
            return node;
        }

        JsonNode setNode = objectNode.get("set");
        if (setNode == null || setNode.isNull() || !setNode.isObject()) {
            ObjectNode newSetNode = objectMapper.createObjectNode();
            newSetNode.put("id", setId);
            objectNode.set("set", newSetNode);
            return objectNode;
        }

        ObjectNode setObject = (ObjectNode) setNode;
        JsonNode idNode = setObject.get("id");
        if (idNode == null || idNode.isNull() || idNode.asText().isBlank()) {
            setObject.put("id", setId);
        }
        return objectNode;
    }

    private String optionalText(JsonNode node, String field) {
        JsonNode valueNode = node.get(field);
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }
        String value = valueNode.asText();
        return value == null || value.isBlank() ? null : value;
    }

    private Integer optionalInt(JsonNode node, String field) {
        JsonNode valueNode = node.get(field);
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }
        return valueNode.asInt();
    }

    private String joinArray(JsonNode node) {
        if (!node.isArray() || node.isEmpty()) {
            return null;
        }
        List<String> values = new ArrayList<>();
        for (JsonNode child : node) {
            String text = child.asText();
            if (text != null && !text.isBlank()) {
                values.add(text);
            }
        }
        return values.isEmpty() ? null : String.join("|", values);
    }

    public record ImportCounters(long cards, long sets) {
    }
}

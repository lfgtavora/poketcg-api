package com.lfgtavora.poketcapi.poketcg_api.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lfgtavora.poketcapi.poketcg_api.config.PokemonDataProperties;

@Component
public class GithubDatasetClient implements DatasetClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final PokemonDataProperties properties;

    public GithubDatasetClient(RestClient restClient, ObjectMapper objectMapper, PokemonDataProperties properties) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public String fetchLatestRevision() {
        String url = "%s/repos/%s/%s/commits/%s".formatted(
                properties.getDataset().getGithubApiBaseUrl(),
                properties.getDataset().getOwner(),
                properties.getDataset().getRepo(),
                properties.getDataset().getBranch());

        Map<?, ?> payload = restClient.get()
                .uri(url)
                .retrieve()
                .body(Map.class);

        JsonNode node = objectMapper.valueToTree(payload);
        JsonNode shaNode = node.get("sha");
        if (shaNode == null || shaNode.isNull() || shaNode.asText().isBlank()) {
            throw new IllegalStateException("Unable to resolve latest dataset revision from GitHub");
        }

        return shaNode.asText();
    }

    @Override
    public InputStream downloadDatasetSnapshot() throws IOException {
        String url = "%s/%s/%s/tar.gz/refs/heads/%s".formatted(
                properties.getDataset().getCodeloadBaseUrl(),
                properties.getDataset().getOwner(),
                properties.getDataset().getRepo(),
                properties.getDataset().getBranch());

        return URI.create(url).toURL().openStream();
    }
}

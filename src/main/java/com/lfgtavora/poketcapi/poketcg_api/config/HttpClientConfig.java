package com.lfgtavora.poketcapi.poketcg_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfig {

    @Bean
    RestClient restClient(RestClient.Builder builder) {
        return builder
                .defaultHeader("User-Agent", "poketcg-local-api")
                .build();
    }
}

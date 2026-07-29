package com.lfgtavora.poketcapi.poketcg_api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.lfgtavora.poketcapi.poketcg_api.domain.CardEntity;
import com.lfgtavora.poketcapi.poketcg_api.domain.SetEntity;
import com.lfgtavora.poketcapi.poketcg_api.repository.CardRepository;
import com.lfgtavora.poketcapi.poketcg_api.repository.SetRepository;
import com.lfgtavora.poketcapi.poketcg_api.service.AutocompleteSearchService;

@SpringBootTest
@AutoConfigureMockMvc
class ApiContractTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private SetRepository setRepository;

    @Autowired
    private AutocompleteSearchService autocompleteSearchService;

    @BeforeEach
    void setupData() {
        cardRepository.deleteAll();
        setRepository.deleteAll();

        SetEntity set = new SetEntity();
        set.setId("base1");
        set.setName("Base");
        set.setSeries("Base");
        set.setPrintedTotal(102);
        set.setTotal(102);
        set.setReleaseDate("1999/01/09");
        set.setUpdatedAt("2025/07/25 23:00:00");
        set.setRawJson("""
                {"id":"base1","name":"Base","series":"Base","printedTotal":102,"total":102,"images":{"symbol":"https://images.pokemontcg.io/base1/symbol.png","logo":"https://images.pokemontcg.io/base1/logo.png"}}
                """);
        set.setSyncedAt(Instant.now());
        setRepository.save(set);

        CardEntity card = new CardEntity();
        card.setId("base1-1");
        card.setName("Alakazam");
        card.setSetId("base1");
        card.setSupertype("Pokemon");
        card.setSubtypes("Stage 2");
        card.setTypes("Psychic");
        card.setUpdatedAt("2025/07/25 23:00:00");
        card.setNumber("1");
        card.setRawJson("""
                {"id":"base1-1","name":"Alakazam","supertype":"Pokemon","number":"1","rarity":"Holo Rare","set":{"id":"base1"},"images":{"small":"https://images.pokemontcg.io/base1/1.png","large":"https://images.pokemontcg.io/base1/1_hires.png"}}
                """);
        card.setSyncedAt(Instant.now());
        cardRepository.save(card);

        autocompleteSearchService.rebuildIndex();
    }

    @Test
    void shouldReturnCardsEnvelopeAndFilters() throws Exception {
        mockMvc.perform(get("/v2/cards")
                        .queryParam("q", "name:Alakazam")
                        .queryParam("page", "1")
                        .queryParam("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.data[0].id").value("base1-1"));
    }

    @Test
    void shouldReturnSetById() throws Exception {
        mockMvc.perform(get("/v2/sets/base1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("base1"))
                .andExpect(jsonPath("$.data.name").value("Base"));
    }

    @Test
    void shouldSelectCardFields() throws Exception {
        mockMvc.perform(get("/v2/cards")
                        .queryParam("q", "set.id:base1")
                        .queryParam("select", "id,name,images,number,supertype,set"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("base1-1"))
                .andExpect(jsonPath("$.data[0].name").value("Alakazam"))
                .andExpect(jsonPath("$.data[0].number").value("1"))
                .andExpect(jsonPath("$.data[0].supertype").value("Pokemon"))
                .andExpect(jsonPath("$.data[0].set.id").value("base1"))
                .andExpect(jsonPath("$.data[0].images.small").value("https://images.pokemontcg.io/base1/1.png"))
                .andExpect(jsonPath("$.data[0].rarity").doesNotExist());

        mockMvc.perform(get("/v2/cards/base1-1").queryParam("select", "id,name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("base1-1"))
                .andExpect(jsonPath("$.data.name").value("Alakazam"))
                .andExpect(jsonPath("$.data.set").doesNotExist())
                .andExpect(jsonPath("$.data.images").doesNotExist());
    }

    @Test
    void shouldSelectSetFields() throws Exception {
        mockMvc.perform(get("/v2/sets").queryParam("select", "id,name,images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("base1"))
                .andExpect(jsonPath("$.data[0].name").value("Base"))
                .andExpect(jsonPath("$.data[0].images.symbol").value("https://images.pokemontcg.io/base1/symbol.png"))
                .andExpect(jsonPath("$.data[0].series").doesNotExist())
                .andExpect(jsonPath("$.data[0].printedTotal").doesNotExist());
    }

    @Test
    void shouldReturnAutocompleteHitsForCardsAndSets() throws Exception {
        mockMvc.perform(get("/v2/search").queryParam("query", "base"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].type").value("set"))
                .andExpect(jsonPath("$.data[0].id").value("base1"))
                .andExpect(jsonPath("$.data[0].name").value("Base"))
                .andExpect(jsonPath("$.data[0].series").value("Base"))
                .andExpect(jsonPath("$.data[0].set").doesNotExist())
                .andExpect(jsonPath("$.data[0].images.symbol").value("https://images.pokemontcg.io/base1/symbol.png"))
                .andExpect(jsonPath("$.data[0].images.logo").value("https://images.pokemontcg.io/base1/logo.png"))
                .andExpect(jsonPath("$.data[0].images.small").doesNotExist())
                .andExpect(jsonPath("$.data[0].images.large").doesNotExist());

        mockMvc.perform(get("/v2/search").queryParam("query", "alak"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].type").value("card"))
                .andExpect(jsonPath("$.data[0].id").value("base1-1"))
                .andExpect(jsonPath("$.data[0].name").value("Alakazam"))
                .andExpect(jsonPath("$.data[0].set.id").value("base1"))
                .andExpect(jsonPath("$.data[0].set.name").value("Base"))
                .andExpect(jsonPath("$.data[0].series").doesNotExist())
                .andExpect(jsonPath("$.data[0].images.small").value("https://images.pokemontcg.io/base1/1.png"))
                .andExpect(jsonPath("$.data[0].images.large").value("https://images.pokemontcg.io/base1/1_hires.png"))
                .andExpect(jsonPath("$.data[0].images.symbol").doesNotExist())
                .andExpect(jsonPath("$.data[0].images.logo").doesNotExist());
    }

    @Test
    void shouldFilterAutocompleteByTypes() throws Exception {
        mockMvc.perform(get("/v2/search")
                        .queryParam("query", "base")
                        .queryParam("types", "card"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(get("/v2/search")
                        .queryParam("query", "base")
                        .queryParam("types", "set"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].type").value("set"))
                .andExpect(jsonPath("$.data[0].id").value("base1"));

        mockMvc.perform(get("/v2/search")
                        .queryParam("query", "alak")
                        .queryParam("types", "card,set"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].type").value("card"));

        mockMvc.perform(get("/v2/search")
                        .queryParam("query", "alak")
                        .queryParam("types", "pokemon"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(400));
    }

    @Test
    void shouldMatchAutocompleteCaseInsensitivelyAndRespectLimit() throws Exception {
        SetEntity jungle = new SetEntity();
        jungle.setId("base2");
        jungle.setName("Jungle");
        jungle.setSeries("Base");
        jungle.setPrintedTotal(64);
        jungle.setTotal(64);
        jungle.setReleaseDate("1999/06/16");
        jungle.setUpdatedAt("2025/07/25 23:00:00");
        jungle.setRawJson("""
                {"id":"base2","name":"Jungle","series":"Base"}
                """);
        jungle.setSyncedAt(Instant.now());
        setRepository.save(jungle);

        CardEntity abra = new CardEntity();
        abra.setId("base1-43");
        abra.setName("Abra");
        abra.setSetId("base1");
        abra.setSupertype("Pokemon");
        abra.setUpdatedAt("2025/07/25 23:00:00");
        abra.setRawJson("""
                {"id":"base1-43","name":"Abra","set":{"id":"base1"}}
                """);
        abra.setSyncedAt(Instant.now());
        cardRepository.save(abra);

        CardEntity alakazamEx = new CardEntity();
        alakazamEx.setId("base2-1");
        alakazamEx.setName("Alakazam");
        alakazamEx.setSetId("base2");
        alakazamEx.setSupertype("Pokemon");
        alakazamEx.setUpdatedAt("2025/07/25 23:00:00");
        alakazamEx.setRawJson("""
                {"id":"base2-1","name":"Alakazam","set":{"id":"base2"}}
                """);
        alakazamEx.setSyncedAt(Instant.now());
        cardRepository.save(alakazamEx);
        autocompleteSearchService.rebuildIndex();

        mockMvc.perform(get("/v2/search")
                        .queryParam("query", "ALA")
                        .queryParam("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].type").value("card"))
                .andExpect(jsonPath("$.data[0].name").value("Alakazam"));

        mockMvc.perform(get("/v2/search").queryParam("query", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(400));
    }

    @Test
    void shouldMatchAutocompleteWithTypos() throws Exception {
        CardEntity pikachu = new CardEntity();
        pikachu.setId("base1-58");
        pikachu.setName("Pikachu");
        pikachu.setSetId("base1");
        pikachu.setSupertype("Pokemon");
        pikachu.setUpdatedAt("2025/07/25 23:00:00");
        pikachu.setRawJson("""
                {"id":"base1-58","name":"Pikachu","set":{"id":"base1"}}
                """);
        pikachu.setSyncedAt(Instant.now());
        cardRepository.save(pikachu);
        autocompleteSearchService.rebuildIndex();

        mockMvc.perform(get("/v2/search").queryParam("query", "picachu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].type").value("card"))
                .andExpect(jsonPath("$.data[0].name").value("Pikachu"));
    }
}

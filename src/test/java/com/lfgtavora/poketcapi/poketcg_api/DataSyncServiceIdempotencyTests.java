package com.lfgtavora.poketcapi.poketcg_api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.lfgtavora.poketcapi.poketcg_api.repository.CardRepository;
import com.lfgtavora.poketcapi.poketcg_api.repository.SetRepository;
import com.lfgtavora.poketcapi.poketcg_api.service.DataSyncService;
import com.lfgtavora.poketcapi.poketcg_api.service.DatasetClient;
import com.lfgtavora.poketcapi.poketcg_api.service.SyncRunResult;

@SpringBootTest(properties = "poketcg.sync.enabled=true")
class DataSyncServiceIdempotencyTests {

    @Autowired
    private DataSyncService dataSyncService;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private SetRepository setRepository;

    @Autowired
    private DatasetClient datasetClient;

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        DatasetClient datasetClient() {
            return new FixedDatasetClient();
        }
    }

    @Test
    void shouldSkipSecondSyncWhenRevisionIsUnchanged() {
        FixedDatasetClient fixed = (FixedDatasetClient) datasetClient;
        // Startup sync may have already downloaded once; this test only cares about the forced run.
        fixed.downloadCount.set(0);

        SyncRunResult firstRun = dataSyncService.syncIfNeeded(true);
        SyncRunResult secondRun = dataSyncService.syncIfNeeded(false);

        assertThat(firstRun.changed()).isTrue();
        assertThat(firstRun.cardsProcessed()).isEqualTo(1);
        assertThat(firstRun.setsProcessed()).isEqualTo(1);

        assertThat(secondRun.changed()).isFalse();
        assertThat(secondRun.status()).isEqualTo("UNCHANGED");
        assertThat(cardRepository.count()).isEqualTo(1);
        assertThat(setRepository.count()).isEqualTo(1);
        assertThat(fixed.downloadCount.get()).isEqualTo(1);
    }

    static class FixedDatasetClient implements DatasetClient {
        private static final String REVISION = "rev-test-1";
        private final byte[] datasetTarGz;
        private final AtomicInteger downloadCount = new AtomicInteger(0);

        FixedDatasetClient() {
            this.datasetTarGz = createDatasetTarGz();
        }

        @Override
        public String fetchLatestRevision() {
            return REVISION;
        }

        @Override
        public InputStream downloadDatasetSnapshot() {
            downloadCount.incrementAndGet();
            return new ByteArrayInputStream(datasetTarGz);
        }

        private static byte[] createDatasetTarGz() {
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                try (GzipCompressorOutputStream gzip = new GzipCompressorOutputStream(byteArrayOutputStream);
                        TarArchiveOutputStream tar = new TarArchiveOutputStream(gzip)) {
                    tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

                    addEntry(tar, "pokemon-tcg-data-master/sets/base1.json",
                            """
                                    {"id":"base1","name":"Base","series":"Base","printedTotal":102,"total":102,"releaseDate":"1999/01/09"}
                                    """);
                    addEntry(tar, "pokemon-tcg-data-master/cards/en/base1-1.json",
                            """
                                    {"id":"base1-1","name":"Alakazam","supertype":"Pokemon","types":["Psychic"],"set":{"id":"base1"}}
                                    """);
                }
                return byteArrayOutputStream.toByteArray();
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to create in-memory dataset archive", exception);
            }
        }

        private static void addEntry(TarArchiveOutputStream tar, String path, String content) throws IOException {
            byte[] bytes = content.strip().getBytes(StandardCharsets.UTF_8);
            TarArchiveEntry entry = new TarArchiveEntry(path);
            entry.setSize(bytes.length);
            tar.putArchiveEntry(entry);
            tar.write(bytes);
            tar.closeArchiveEntry();
        }
    }
}

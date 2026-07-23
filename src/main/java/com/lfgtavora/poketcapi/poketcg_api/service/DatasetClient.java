package com.lfgtavora.poketcapi.poketcg_api.service;

import java.io.IOException;
import java.io.InputStream;

public interface DatasetClient {

    String fetchLatestRevision();

    InputStream downloadDatasetSnapshot() throws IOException;
}

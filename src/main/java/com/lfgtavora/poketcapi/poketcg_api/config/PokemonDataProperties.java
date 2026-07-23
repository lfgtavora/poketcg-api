package com.lfgtavora.poketcapi.poketcg_api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "poketcg")
public class PokemonDataProperties {

    private final Sync sync = new Sync();
    private final Dataset dataset = new Dataset();

    public Sync getSync() {
        return sync;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public static class Sync {
        private boolean enabled = true;
        private String cron = "0 0 4 * * *";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }
    }

    public static class Dataset {
        private String owner = "PokemonTCG";
        private String repo = "pokemon-tcg-data";
        private String branch = "master";
        private String githubApiBaseUrl = "https://api.github.com";
        private String codeloadBaseUrl = "https://codeload.github.com";

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public String getRepo() {
            return repo;
        }

        public void setRepo(String repo) {
            this.repo = repo;
        }

        public String getBranch() {
            return branch;
        }

        public void setBranch(String branch) {
            this.branch = branch;
        }

        public String getGithubApiBaseUrl() {
            return githubApiBaseUrl;
        }

        public void setGithubApiBaseUrl(String githubApiBaseUrl) {
            this.githubApiBaseUrl = githubApiBaseUrl;
        }

        public String getCodeloadBaseUrl() {
            return codeloadBaseUrl;
        }

        public void setCodeloadBaseUrl(String codeloadBaseUrl) {
            this.codeloadBaseUrl = codeloadBaseUrl;
        }
    }
}

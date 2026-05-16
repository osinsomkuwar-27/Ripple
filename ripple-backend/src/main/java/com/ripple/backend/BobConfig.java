package com.ripple.backend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BobConfig {
    @Value("${bob.api.key}")
    private String apiKey;

    @Value("${bob.repo.path}")
    private String repoPath;

    public String getApiKey() { return apiKey; }
    public String getRepoPath() { return repoPath; }
}

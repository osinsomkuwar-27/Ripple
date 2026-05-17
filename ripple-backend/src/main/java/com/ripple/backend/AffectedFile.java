package com.ripple.backend;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AffectedFile {

    @JsonProperty("file_path")
    private String filePath;

    @JsonAlias("path")

    private String reason;

    @JsonProperty("risk_tier")
    private String riskTier;

    @JsonProperty("risk_score")
    private int riskScore;

    @JsonProperty("likely_broken_lines")
    private List<Integer> likelyBrokenLines;

    @JsonProperty("verified")
    private boolean verified = false;

    @JsonProperty("cascade_chain")
    private List<String> cascadeChain = List.of();

    public AffectedFile() {}

    public AffectedFile(String filePath, String reason, String riskTier,
                        int riskScore, List<Integer> likelyBrokenLines) {
        this.filePath = filePath;
        this.reason = reason;
        this.riskTier = riskTier;
        this.riskScore = riskScore;
        this.likelyBrokenLines = likelyBrokenLines;
    }

    public String getFilePath() { return filePath; }
    public void setFilePath(String f) { this.filePath = f; }

    public String getReason() { return reason; }
    public void setReason(String r) { this.reason = r; }

    public String getRiskTier() { return riskTier; }
    public void setRiskTier(String r) { this.riskTier = r; }

    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int s) { this.riskScore = s; }

    public List<Integer> getLikelyBrokenLines() { return likelyBrokenLines; }
    public void setLikelyBrokenLines(List<Integer> l) { this.likelyBrokenLines = l; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean v) { this.verified = v; }

    public List<String> getCascadeChain() { return cascadeChain; }
    public void setCascadeChain(List<String> c) { this.cascadeChain = c; }
}
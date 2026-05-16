package com.ripple.backend;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AffectedFile {
    private String file;

    @JsonAlias("file_path")
    private String filePath;

    private String reason;
    private String risk;

    @JsonAlias("risk_tier")
    private String riskTier;

    @JsonAlias("risk_score")
    private Integer riskScore;

    public AffectedFile() {}

    public AffectedFile(String file, String reason, String risk) {
        this.file = file;
        this.reason = reason;
        this.risk = risk;
    }

    // Unified getter — returns whichever is available
    public String getFile() {
        return file != null ? file : filePath;
    }
    public void setFile(String f) { this.file = f; }
    public void setFilePath(String f) { this.filePath = f; }

    public String getReason() { return reason; }
    public void setReason(String r) { this.reason = r; }

    // Unified risk getter — prefers risk, falls back to riskTier
    public String getRisk() {
        return risk != null ? risk : riskTier;
    }
    public void setRisk(String r) { this.risk = r; }
    public void setRiskTier(String r) { this.riskTier = r; }
    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer s) { this.riskScore = s; }
}

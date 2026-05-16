package com.ripple.backend;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RippleResponse {

    private String intent;

    @JsonProperty("affected_files")
    private List<AffectedFile> affectedFiles;

    @JsonProperty("total_affected")
    private int totalAffected;

    @JsonProperty("high_risk_count")
    private int highRiskCount;

    @JsonProperty("medium_risk_count")
    private int mediumRiskCount;

    @JsonProperty("low_risk_count")
    private int lowRiskCount;

    public RippleResponse() {}

    public RippleResponse(String intent, List<AffectedFile> affectedFiles,
                          int totalAffected, int highRiskCount,
                          int mediumRiskCount, int lowRiskCount) {
        this.intent = intent;
        this.affectedFiles = affectedFiles;
        this.totalAffected = totalAffected;
        this.highRiskCount = highRiskCount;
        this.mediumRiskCount = mediumRiskCount;
        this.lowRiskCount = lowRiskCount;
    }

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }

    public List<AffectedFile> getAffectedFiles() { return affectedFiles; }
    public void setAffectedFiles(List<AffectedFile> a) { this.affectedFiles = a; }

    public int getTotalAffected() { return totalAffected; }
    public void setTotalAffected(int t) { this.totalAffected = t; }

    public int getHighRiskCount() { return highRiskCount; }
    public void setHighRiskCount(int h) { this.highRiskCount = h; }

    public int getMediumRiskCount() { return mediumRiskCount; }
    public void setMediumRiskCount(int m) { this.mediumRiskCount = m; }

    public int getLowRiskCount() { return lowRiskCount; }
    public void setLowRiskCount(int l) { this.lowRiskCount = l; }
}
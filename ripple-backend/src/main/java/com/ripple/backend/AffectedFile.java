package com.ripple.backend;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AffectedFile {
    private String file;
    private String reason;
    private String risk;

    public AffectedFile() {}

    public AffectedFile(String file, String reason, String risk) {
        this.file = file;
        this.reason = reason;
        this.risk = risk;
    }
    public String getFile() { return file; }
    public void setFile(String f) { this.file = f; }
    public String getReason() { return reason; }
    public void setReason(String r) { this.reason = r; }
    public String getRisk() { return risk; }
    public void setRisk(String r) { this.risk = r; }
}

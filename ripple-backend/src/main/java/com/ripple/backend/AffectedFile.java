package com.ripple.backend;

public class AffectedFile {
    private String file;
    private String reason;
    private String risk;

    public AffectedFile(String file, String reason, String risk) {
        this.file = file;
        this.reason = reason;
        this.risk = risk;
    }
    public String getFile() { return file; }
    public String getReason() { return reason; }
    public String getRisk() { return risk; }
}

package com.ripple.backend;

import java.util.List;

public class RippleResponse {
    private String changedFile;
    private String summary;
    private List<AffectedFile> affectedFiles;

    public RippleResponse(String changedFile, String summary, List<AffectedFile> affectedFiles) {
        this.changedFile = changedFile;
        this.summary = summary;
        this.affectedFiles = affectedFiles;
    }
    public String getChangedFile() { return changedFile; }
    public String getSummary() { return summary; }
    public List<AffectedFile> getAffectedFiles() { return affectedFiles; }
}

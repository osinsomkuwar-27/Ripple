package com.ripple.backend;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RippleResponse {
    private String changedFile;
    private String summary;
    private List<AffectedFile> affectedFiles;

    public RippleResponse() {}

    public RippleResponse(String changedFile, String summary, List<AffectedFile> affectedFiles) {
        this.changedFile = changedFile;
        this.summary = summary;
        this.affectedFiles = affectedFiles;
    }
    public String getChangedFile() { return changedFile; }
    public void setChangedFile(String c) { this.changedFile = c; }
    public String getSummary() { return summary; }
    public void setSummary(String s) { this.summary = s; }
    public List<AffectedFile> getAffectedFiles() { return affectedFiles; }
    public void setAffectedFiles(List<AffectedFile> a) { this.affectedFiles = a; }
}

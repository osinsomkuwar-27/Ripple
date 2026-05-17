package com.ripple.backend;

public class AnalyzeRequest {
    private String changedFile;
    private String changeDescription;
    private String repoUrl;

    public String getChangedFile() { return changedFile; }
    public void setChangedFile(String changedFile) { this.changedFile = changedFile; }
    public String getChangeDescription() { return changeDescription; }
    public void setChangeDescription(String changeDescription) { this.changeDescription = changeDescription; }
    public String getRepoUrl() { return repoUrl; }
    public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }
}

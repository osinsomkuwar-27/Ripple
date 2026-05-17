package com.ripple.backend;

import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;

@Service
public class BobService {

    private final BobConfig config;

    public BobService(BobConfig config) {
        this.config = config;
    }

    public String cloneIfNeeded(String repoUrl) throws Exception {
        if (repoUrl == null || repoUrl.isBlank()) {
            return config.getRepoPath(); // fallback to default
        }

        // Cache by URL hash
        String hash = md5(repoUrl).substring(0, 8);
        String clonedPath = System.getProperty("user.home") + "/.ripple/cloned/" + hash;
        File cloneDir = new File(clonedPath);

        if (cloneDir.exists() && cloneDir.isDirectory()) {
            System.out.println("=== USING CACHED CLONE: " + clonedPath);
            return clonedPath;
        }

        System.out.println("=== CLONING: " + repoUrl + " → " + clonedPath);
        cloneDir.mkdirs();

        String os = System.getProperty("os.name").toLowerCase();
        String[] cmd = os.contains("win")
            ? new String[]{"cmd.exe", "/c", "git", "clone", "--depth=1", repoUrl, clonedPath}
            : new String[]{"git", "clone", "--depth=1", repoUrl, clonedPath};

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line).append("\n");
        }

        int exitCode = process.waitFor();
        System.out.println("=== GIT CLONE EXIT: " + exitCode);
        System.out.println("=== GIT OUTPUT: " + out);

        if (exitCode != 0) {
            cloneDir.delete();
            throw new RuntimeException("git clone failed: " + out);
        }

        return clonedPath;
    }

    private String md5(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hash = md.digest(input.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public String analyze(String changedFile, String changeDescription, String repoPath) throws Exception {

        String prompt = "You are a code impact analyzer for this Java project. "
        + "The file " + changedFile + " is being changed: " + changeDescription + ". "
        + "Analyze which other files in this project will be affected by this change. "
        + "CRITICAL: Return ONLY a single JSON object — NOT an array. "
        + "Use lowercase for risk_tier values. "
        + "Include risk_score as a number from 0-100 for each affected file. "
        + "Limit likely_broken_lines to maximum 5 specific line numbers. "
        + "You MUST include all these top-level fields: intent, affected_files, total_affected, high_risk_count, medium_risk_count, low_risk_count.\n"
        + "{\n"
        + "  \"intent\": \"" + changeDescription + "\",\n"
        + "  \"affected_files\": [\n"
        + "    {\n"
        + "      \"file_path\": \"src/path/to/File.java\",\n"
        + "      \"risk_tier\": \"high\",\n"
        + "      \"risk_score\": 85,\n"
        + "      \"reason\": \"why this file is affected\",\n"
        + "      \"likely_broken_lines\": [42, 87]\n"
        + "    }\n"
        + "  ],\n"
        + "  \"total_affected\": 1,\n"
        + "  \"high_risk_count\": 1,\n"
        + "  \"medium_risk_count\": 0,\n"
        + "  \"low_risk_count\": 0\n"
        + "}";

        String os = System.getProperty("os.name").toLowerCase();
        System.out.println("=== OS DETECTED: " + os);
        System.out.println("=== REPO PATH: " + repoPath);
        System.out.println("=== API KEY starts with: " + config.getApiKey().substring(0, Math.min(20, config.getApiKey().length())));

        String[] commands = os.contains("win")
            ? new String[]{"cmd.exe", "/c", "bob", "--accept-license", "--auth-method", "api-key", "-p", prompt}
            : new String[]{"bob", "--accept-license", "--auth-method", "api-key", "-p", prompt};

        ProcessBuilder pb = new ProcessBuilder(commands);
        pb.directory(new File(repoPath));
        pb.environment().put("BOBSHELL_API_KEY", config.getApiKey());
        pb.redirectErrorStream(true);

        Process process = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) output.append(line).append("\n");

        int exitCode = process.waitFor();
        System.out.println("=== EXIT CODE: " + exitCode);

        if (exitCode != 0) throw new RuntimeException("Bob CLI failed: " + output);

        return output.toString();
    }
}
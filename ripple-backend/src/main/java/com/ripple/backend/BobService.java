package com.ripple.backend;

import org.springframework.stereotype.Service;
import java.io.*;

@Service
public class BobService {

    private final BobConfig config;

    public BobService(BobConfig config) {
        this.config = config;
    }

    public String analyze(String changedFile, String changeDescription) throws Exception {

        String prompt = "You are a code impact analyzer for this Spring PetClinic Java project. "
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

        ProcessBuilder pb = new ProcessBuilder(
            "cmd.exe", "/c", "bob",
            "--accept-license",
            "--auth-method", "api-key",
            prompt
        );

        pb.directory(new File(config.getRepoPath()));

        // Set env var explicitly
        pb.environment().put("BOBSHELL_API_KEY", config.getApiKey());

        // Also inherit current environment
        pb.redirectErrorStream(true);

        System.out.println("=== REPO PATH: " + config.getRepoPath());
        System.out.println("=== API KEY starts with: " + config.getApiKey().substring(0, 20));

        Process process = pb.start();

        BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream())
        );

        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        int exitCode = process.waitFor();
        System.out.println("=== EXIT CODE: " + exitCode);
        System.out.println("=== BOB OUTPUT: " + output);

        return output.toString();
    }
}

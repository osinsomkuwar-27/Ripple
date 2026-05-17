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

        // Cross-platform Bob CLI execution with fallback strategy
        String os = System.getProperty("os.name").toLowerCase();
        System.out.println("=== OS DETECTED: " + os);
        System.out.println("=== REPO PATH: " + config.getRepoPath());
        System.out.println("=== API KEY starts with: " + config.getApiKey().substring(0, Math.min(20, config.getApiKey().length())));

        ProcessBuilder pb;
        String[] commands;
        
        if (os.contains("win")) {
            commands = new String[]{"cmd.exe", "/c", "bob", "--accept-license", "--auth-method", "api-key", "-p", prompt};
        } else {
            // macOS/Linux - try direct execution first
            commands = new String[]{"bob", "--accept-license", "--auth-method", "api-key", "-p", prompt};
        }
        
        pb = new ProcessBuilder(commands);
        pb.directory(new File(config.getRepoPath()));
        pb.environment().put("BOBSHELL_API_KEY", config.getApiKey());
        pb.redirectErrorStream(true);

        Process process = null;
        StringBuilder output = new StringBuilder();
        int exitCode = -1;
        
        try {
            process = pb.start();
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            exitCode = process.waitFor();
            System.out.println("=== EXIT CODE: " + exitCode);
            System.out.println("=== BOB OUTPUT LENGTH: " + output.length());
            
            if (exitCode != 0) {
                System.err.println("=== BOB FAILED WITH EXIT CODE: " + exitCode);
                System.err.println("=== BOB ERROR OUTPUT: " + output);
                String truncatedOutput = output.length() > 500
                    ? output.substring(0, 500) + "..."
                    : output.toString();
                throw new RuntimeException(
                    "Bob CLI failed with exit code " + exitCode + ". " +
                    "Check Node/Bob compatibility and ensure Bob CLI is installed. " +
                    "Output: " + truncatedOutput
                );
            }
            
        } catch (IOException e) {
            String errorMsg = "Failed to execute Bob CLI. Ensure 'bob' is installed and in PATH. OS: " + os + ", Error: " + e.getMessage();
            System.err.println("=== " + errorMsg);
            throw new RuntimeException(errorMsg, e);
        }

        return output.toString();
    }
}

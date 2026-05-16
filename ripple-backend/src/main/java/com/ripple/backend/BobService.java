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
            + "Search the codebase for all references and dependencies. "
            + "Return ONLY valid JSON in exactly this format, no explanation, no markdown: "
            + "{\"changedFile\":\"" + changedFile + "\","
            + "\"summary\":\"N files affected across M modules\","
            + "\"affectedFiles\":["
            + "{\"file\":\"FileName.java\",\"reason\":\"why affected\",\"risk\":\"HIGH\"}"
            + "]}";

        ProcessBuilder pb = new ProcessBuilder(
            "bob",
            "--accept-license",
            "--auth-method", "api-key",
            "-p", prompt
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

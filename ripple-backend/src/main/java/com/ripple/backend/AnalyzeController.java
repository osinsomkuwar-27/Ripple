package com.ripple.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ripple.backend.parser.JavaAstParserService;
import com.ripple.backend.parser.ParserAdjacency;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AnalyzeController {

    private final BobService bobService;
    private final BobConfig bobConfig;
    private final JavaAstParserService astParser;
    private final ObjectMapper mapper = new ObjectMapper();

    public AnalyzeController(BobService bobService, BobConfig bobConfig) {
        this.bobService = bobService;
        this.bobConfig = bobConfig;
        this.astParser = new JavaAstParserService();
    }

    @PostMapping("/analyze")
    public RippleResponse analyze(@RequestBody AnalyzeRequest req) throws Exception {

        String raw = bobService.analyze(req.getChangedFile(), req.getChangeDescription());

        RippleResponse response = null;

        try {
            String marker = "---output---";

            List<Integer> markers = new java.util.ArrayList<>();
            int pos = 0;
            while ((pos = raw.indexOf(marker, pos)) >= 0) {
                markers.add(pos);
                pos += marker.length();
            }

            System.out.println("=== TOTAL MARKERS FOUND: " + markers.size());

            for (int i = markers.size() - 2; i >= 0; i--) {
                int start = markers.get(i) + marker.length();
                int end = markers.get(i + 1);
                String between = raw.substring(start, end).trim();
                int jsonStart = between.indexOf('{');
                int jsonEnd = between.lastIndexOf('}') + 1;
                if (jsonStart >= 0 && jsonEnd > 0) {
                    try {
                        response = mapper.readValue(between.substring(jsonStart, jsonEnd), RippleResponse.class);
                        break;
                    } catch (Exception e) {
                        System.out.println("=== SKIPPING PAIR " + i + ": " + e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("=== PARSE ERROR: " + e.getMessage());
        }

        if (response == null) {
            return new RippleResponse(req.getChangeDescription(), List.of(), 0, 0, 0, 0);
        }

        // ── Step 1: AST verification ──────────────────────────────────────────
        try {
            Path repoPath = Path.of(bobConfig.getRepoPath());
            ParserAdjacency adjacency = astParser.buildAdjacency(repoPath);
            Set<String> realClasses = new HashSet<>(adjacency.getAllClasses());

            System.out.println("=== AST CLASSES FOUND: " + realClasses.size());

            for (AffectedFile af : response.getAffectedFiles()) {
                // Extract simple class name from path e.g. "src/.../Owner.java" → "Owner"
                String fileName = af.getFilePath();
                if (fileName == null || fileName.isBlank()) {
                    System.out.println("=== SKIPPING file with null path");
                    continue;
                }
                String className = fileName
                        .substring(fileName.lastIndexOf('/') + 1)
                        .replace(".java", "");

                boolean verified = realClasses.contains(className);
                af.setVerified(verified);

                // Build cascade chain from AST dependents
                List<String> cascade = adjacency.getDependencies()
                        .getOrDefault(className, List.of());
                af.setCascadeChain(cascade);

                System.out.println("=== " + className + " verified=" + verified
                        + " cascade=" + cascade.size());
            }

        } catch (Exception e) {
            System.out.println("=== AST VERIFICATION FAILED: " + e.getMessage());
            // Non-fatal — return Bob's response as-is
        }

        return response;
    }

    @GetMapping("/health")
    public String health() { return "Ripple backend running"; }
}
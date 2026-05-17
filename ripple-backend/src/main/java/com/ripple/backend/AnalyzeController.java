package com.ripple.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ripple.backend.parser.JavaAstParserService;
import com.ripple.backend.parser.ParserAdjacency;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AnalyzeController {

    private final BobService bobService;
    private final BobConfig bobConfig;
    private final AnalysisStore store;
    private final JavaAstParserService astParser;
    private final ObjectMapper mapper = new ObjectMapper();

    public AnalyzeController(BobService bobService, BobConfig bobConfig, AnalysisStore store) {
        this.bobService = bobService;
        this.bobConfig = bobConfig;
        this.store = store;
        this.astParser = new JavaAstParserService();
    }

    @PostMapping("/analyze")
    public Map<String, Object> analyze(@RequestBody AnalyzeRequest req) throws Exception {
        String raw = bobService.analyze(req.getChangedFile(), req.getChangeDescription());
        RippleResponse response = parseResponse(raw, req.getChangedFile(), req.getChangeDescription());
        String id = store.save(response);
        return Map.of(
                "id", id,
                "result", response
        );
    }

    @GetMapping("/analysis/{id}")
    public ResponseEntity<RippleResponse> getById(@PathVariable String id) {
        RippleResponse response = store.get(id);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public String health() {
        return "Ripple backend running";
    }

    private RippleResponse parseResponse(String raw, String changedFile, String fallbackIntent) {
        RippleResponse response = null;
        try {
            String marker = "---output---";
            List<Integer> markers = new java.util.ArrayList<>();
            int pos = 0;
            while ((pos = raw.indexOf(marker, pos)) >= 0) {
                markers.add(pos);
                pos += marker.length();
            }

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
                    } catch (Exception ignored) {
                        // keep scanning older output blocks
                    }
                }
            }

            if (response == null) {
                int start = raw.indexOf('{');
                int end = raw.lastIndexOf('}') + 1;
                if (start >= 0 && end > 0) {
                    response = mapper.readValue(raw.substring(start, end), RippleResponse.class);
                }
            }
        } catch (Exception ignored) {
            // fallback below
        }

        if (response == null) {
            return new RippleResponse(fallbackIntent, List.of(), 0, 0, 0, 0);
        }

        // AST verification/enrichment is best-effort and non-fatal.
        try {
            Path repoPath = Path.of(bobConfig.getRepoPath());
            Path javaRoot = repoPath.resolve("src/main/java/org/springframework/samples/petclinic");
            ParserAdjacency adjacency = astParser.buildAdjacency(javaRoot);
            Set<String> realClasses = new HashSet<>(adjacency.getAllClasses());

            for (AffectedFile af : response.getAffectedFiles()) {
                String filePath = af.getFilePath();
                if (filePath == null || filePath.isBlank() || !filePath.endsWith(".java")) {
                    af.setVerified(false);
                    af.setCascadeChain(List.of());
                    continue;
                }

                String className = filePath.substring(filePath.lastIndexOf('/') + 1).replace(".java", "");
                boolean verified = realClasses.contains(className);
                af.setVerified(verified);
                af.setCascadeChain(adjacency.getDependents().getOrDefault(className, List.of()));
            }
        } catch (Exception ignored) {
            // non-fatal, return Bob response as-is
        }

        if (response.getIntent() == null || response.getIntent().isBlank()) {
            response.setIntent(fallbackIntent);
        }

        return response;
    }
}

package com.ripple.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AnalyzeController {

    private final BobService bobService;
    private final AnalysisStore store;
    private final ObjectMapper mapper = new ObjectMapper();

    public AnalyzeController(BobService bobService, AnalysisStore store) {
        this.bobService = bobService;
        this.store = store;
    }

    @PostMapping("/analyze")
    public Map<String, Object> analyze(@RequestBody AnalyzeRequest req) throws Exception {

        String raw = bobService.analyze(
            req.getChangedFile(),
            req.getChangeDescription()
        );

        RippleResponse response = parseResponse(raw, req.getChangedFile());

        // Save and return with ID
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
    public String health() { return "Ripple backend running"; }

    private RippleResponse parseResponse(String raw, String changedFile) {
        try {
            String marker = "---output---";
            int firstMarker = raw.indexOf(marker);
            int secondMarker = raw.indexOf(marker, firstMarker + marker.length());

            if (firstMarker >= 0 && secondMarker >= 0) {
                String between = raw.substring(firstMarker + marker.length(), secondMarker).trim();
                int start = between.indexOf('{');
                int end = between.lastIndexOf('}') + 1;
                if (start >= 0 && end > 0) {
                    return mapper.readValue(between.substring(start, end), RippleResponse.class);
                }
            }

            int start = raw.indexOf('{');
            int end = raw.lastIndexOf('}') + 1;
            if (start >= 0 && end > 0) {
                return mapper.readValue(raw.substring(start, end), RippleResponse.class);
            }
        } catch (Exception e) {
            System.out.println("Parse error: " + e.getMessage());
        }
        return new RippleResponse(changedFile, "Analysis failed", List.of());
    }
}
// test trigger Sat May 16 18:24:43 UTC 2026

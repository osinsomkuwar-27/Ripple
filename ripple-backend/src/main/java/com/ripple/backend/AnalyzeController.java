package com.ripple.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AnalyzeController {

    private final BobService bobService;
    private final ObjectMapper mapper = new ObjectMapper();

    public AnalyzeController(BobService bobService) {
        this.bobService = bobService;
    }

    @PostMapping("/analyze")
    public RippleResponse analyze(@RequestBody AnalyzeRequest req) throws Exception {

        String raw = bobService.analyze(req.getChangedFile(), req.getChangeDescription());

        try {
            String marker = "---output---";
            
            // Find ALL marker positions
            int pos = 0;
            java.util.List<Integer> markers = new java.util.ArrayList<>();
            while ((pos = raw.indexOf(marker, pos)) >= 0) {
                markers.add(pos);
                pos += marker.length();
            }

            System.out.println("=== TOTAL MARKERS FOUND: " + markers.size());

            // Try each consecutive pair, starting from the last pair
            for (int i = markers.size() - 2; i >= 0; i--) {
                int start = markers.get(i) + marker.length();
                int end = markers.get(i + 1);
                String between = raw.substring(start, end).trim();
                int jsonStart = between.indexOf('{');
                int jsonEnd = between.lastIndexOf('}') + 1;
                if (jsonStart >= 0 && jsonEnd > 0) {
                    try {
                        return mapper.readValue(between.substring(jsonStart, jsonEnd), RippleResponse.class);
                    } catch (Exception e) {
                        System.out.println("=== SKIPPING PAIR " + i + ": " + e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("=== PARSE ERROR: " + e.getMessage());
        }

        return new RippleResponse(req.getChangeDescription(), List.of(), 0, 0, 0, 0);
    }

    @GetMapping("/health")
    public String health() { return "Ripple backend running"; }
}
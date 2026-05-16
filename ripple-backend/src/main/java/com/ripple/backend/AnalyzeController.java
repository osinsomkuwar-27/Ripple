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

        String raw = bobService.analyze(
            req.getChangedFile(),
            req.getChangeDescription()
        );

        try {
            // Bob wraps output between ---output--- markers, extract the last one
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

            // Fallback: just find JSON anywhere in output
            int start = raw.indexOf('{');
            int end = raw.lastIndexOf('}') + 1;
            if (start >= 0 && end > 0) {
                return mapper.readValue(raw.substring(start, end), RippleResponse.class);
            }

        } catch (Exception e) {
            System.out.println("Parse error: " + e.getMessage());
        }

        return new RippleResponse(req.getChangeDescription(), List.of(), 0, 0, 0, 0);
    }

    @GetMapping("/health")
    public String health() { return "Ripple backend running"; }
}

package com.ripple.backend;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AnalyzeController {

    @PostMapping("/analyze")
    public RippleResponse analyze(@RequestBody AnalyzeRequest req) {
        // Mock data for now — Bob integration comes in Phase 2
        List<AffectedFile> affected = List.of(
            new AffectedFile("OwnerRepository.java", "Queries reference lastName field", "HIGH"),
            new AffectedFile("OwnerController.java", "Uses Owner.getLastName()", "HIGH"),
            new AffectedFile("OwnerValidator.java", "Validates lastName not empty", "MEDIUM"),
            new AffectedFile("addOwnerForm.html", "Template renders owner.lastName", "MEDIUM"),
            new AffectedFile("findOwners.html", "Search form uses lastName", "LOW"),
            new AffectedFile("OwnerTests.java", "Tests assert on lastName field", "HIGH")
        );
        return new RippleResponse(
            req.getChangedFile(),
            affected.size() + " files affected across 3 modules",
            affected
        );
    }

    @GetMapping("/health")
    public String health() {
        return "Ripple backend is running";
    }
}

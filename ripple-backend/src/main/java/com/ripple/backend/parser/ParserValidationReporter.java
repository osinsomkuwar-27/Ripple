package com.ripple.backend.parser;

import java.util.List;
import java.util.Map;

public class ParserValidationReporter {
    public String buildReport(ParserAdjacency adjacency) {
        StringBuilder sb = new StringBuilder();

        sb.append("=======================================================\n");
        sb.append("ALL CLASSES FOUND\n");
        sb.append("=======================================================\n");
        for (String cls : adjacency.getAllClasses()) {
            sb.append("  ").append(cls).append('\n');
        }

        List<String> ownerRipples = adjacency.getDependents().getOrDefault("Owner", List.of());
        sb.append('\n');
        sb.append("=======================================================\n");
        sb.append("HERO DEMO — What depends on Owner?\n");
        sb.append("=======================================================\n");
        sb.append("Owner has ").append(ownerRipples.size()).append(" dependents:\n\n");
        for (String cls : ownerRipples) {
            sb.append("  => ").append(cls).append('\n');
        }
        if (ownerRipples.size() < 8) {
            sb.append("\nWARNING: fewer than 8 ripples found. Parser may need tuning.\n");
        } else {
            sb.append("\nGREAT: ").append(ownerRipples.size()).append(" ripples. Demo will look impressive.\n");
        }

        sb.append('\n');
        sb.append("=======================================================\n");
        sb.append("FULL DEPENDENCY MAP (what each class uses)\n");
        sb.append("=======================================================\n");
        for (Map.Entry<String, List<String>> entry : adjacency.getDependencies().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                sb.append("  ").append(entry.getKey()).append('\n');
                for (String dep : entry.getValue()) {
                    sb.append("      --> ").append(dep).append('\n');
                }
            }
        }

        sb.append('\n');
        sb.append("=======================================================\n");
        sb.append("FULL DEPENDENTS MAP (what uses each class)\n");
        sb.append("=======================================================\n");
        for (Map.Entry<String, List<String>> entry : adjacency.getDependents().entrySet()) {
            sb.append("  ").append(entry.getKey())
                    .append("  (").append(entry.getValue().size()).append(" dependents)\n");
            for (String use : entry.getValue()) {
                sb.append("      <-- ").append(use).append('\n');
            }
        }

        return sb.toString();
    }
}

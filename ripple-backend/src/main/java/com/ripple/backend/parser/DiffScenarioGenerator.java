package com.ripple.backend.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DiffScenarioGenerator {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public void generateAll(Path petClinicRepoRoot, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        List<Scenario> scenarios = List.of(
                new Scenario(
                        "scenario_1_owner_rename",
                        "Rename address field to streetAddress in Owner.java",
                        "src/main/java/org/springframework/samples/petclinic/owner/Owner.java",
                        "private String address;",
                        "private String streetAddress;"),
                new Scenario(
                        "scenario_2_visit_field",
                        "Rename date field to visitDate in Visit.java",
                        "src/main/java/org/springframework/samples/petclinic/owner/Visit.java",
                        "private LocalDate date;",
                        "private LocalDate visitDate;"),
                new Scenario(
                        "scenario_3_vet_repository",
                        "Add new query method to VetRepository.java",
                        "src/main/java/org/springframework/samples/petclinic/vet/VetRepository.java",
                        "Collection<Vet> findAll() throws DataAccessException;",
                        "Collection<Vet> findBySpecialty(String specialty) throws DataAccessException;"));

        for (Scenario s : scenarios) {
            Path target = petClinicRepoRoot.resolve(s.filePath);
            if (!Files.exists(target)) {
                throw new IOException("Missing target file for scenario " + s.name + ": " + target);
            }

            List<String> lines = Files.readAllLines(target);
            int index = findLine(lines, s.oldLine);
            String diff = buildDiff(s.filePath, lines, index, s.oldLine, s.newLine);
            Path diffFile = outputDir.resolve(s.name + ".diff");
            Files.writeString(diffFile, diff);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("pr_number", 42);
            payload.put("pr_title", s.description);
            payload.put("base_branch", "main");
            payload.put("head_branch", "feature/" + s.name);
            payload.put("changed_files", List.of(s.filePath));
            payload.put("diff_file", diffFile.getFileName().toString());
            payload.put("repo", "spring-petclinic");

            Path payloadFile = outputDir.resolve(s.name + "_pr_payload.json");
            OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(payloadFile.toFile(), payload);
        }
    }

    private int findLine(List<String> lines, String oldLine) {
        String trimmedOld = oldLine.trim();
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).trim().contains(trimmedOld)) {
                return i;
            }
        }
        return -1;
    }

    private String buildDiff(String filePath, List<String> lines, int idx, String oldLine, String newLine) {
        StringBuilder sb = new StringBuilder();
        sb.append("--- a/").append(filePath).append('\n');
        sb.append("+++ b/").append(filePath).append('\n');

        if (idx < 0) {
            sb.append("@@ -1,3 +1,3 @@\n");
            sb.append(" // Placeholder diff - original line not found\n");
            sb.append("-").append(oldLine).append('\n');
            sb.append("+").append(newLine).append('\n');
            return sb.toString();
        }

        int start = Math.max(0, idx - 3);
        int end = Math.min(lines.size(), idx + 4);
        sb.append("@@ -").append(start + 1).append(",").append(end - start)
                .append(" +").append(start + 1).append(",").append(end - start).append(" @@\n");

        List<String> segment = new ArrayList<>(lines.subList(start, end));
        for (int i = 0; i < segment.size(); i++) {
            int srcIdx = start + i;
            if (srcIdx == idx) {
                sb.append("-    ").append(oldLine.trim()).append('\n');
                sb.append("+    ").append(newLine.trim()).append('\n');
            } else {
                sb.append(" ").append(segment.get(i)).append('\n');
            }
        }
        return sb.toString();
    }

    private record Scenario(String name, String description, String filePath, String oldLine, String newLine) {}
}

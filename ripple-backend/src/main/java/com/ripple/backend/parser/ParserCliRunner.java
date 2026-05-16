package com.ripple.backend.parser;

import java.nio.file.Files;
import java.nio.file.Path;

public class ParserCliRunner {
    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "all";
        Path repoRoot = Path.of(args.length > 1 ? args[1] : ".").toAbsolutePath().normalize();
        Path parserOutputDir = repoRoot.resolve("parser");
        Path javaSourceBase = repoRoot.resolve("spring-petclinic/src/main/java/org/springframework/samples/petclinic");

        JavaAstParserService parser = new JavaAstParserService();

        if ("adjacency".equals(mode) || "all".equals(mode) || "validate".equals(mode)) {
            ParserAdjacency adjacency = parser.buildAdjacency(javaSourceBase);
            parser.writeAdjacency(parserOutputDir.resolve("adjacency.json"), adjacency);
            System.out.println("Wrote: " + parserOutputDir.resolve("adjacency.json"));

            if ("validate".equals(mode) || "all".equals(mode)) {
                String report = new ParserValidationReporter().buildReport(adjacency);
                Path summaryPath = parserOutputDir.resolve("summary.txt");
                Files.writeString(summaryPath, report);
                System.out.println("Wrote: " + summaryPath);
            }
        }

        if ("generate-diff".equals(mode) || "all".equals(mode)) {
            new DiffScenarioGenerator().generateAll(repoRoot.resolve("spring-petclinic"), parserOutputDir);
            System.out.println("Wrote scenario diffs and payloads to: " + parserOutputDir);
        }
    }
}

package com.ripple.backend.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JavaAstParserService {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ParserAdjacency buildAdjacency(Path javaSourceBase) throws IOException {

        // ── Fix: set language level to Java 21 before parsing ──────────────
        ParserConfiguration config = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        StaticJavaParser.setConfiguration(config);
        // ───────────────────────────────────────────────────────────────────

        List<Path> javaFiles;
        try (Stream<Path> stream = Files.walk(javaSourceBase)) {
            javaFiles = stream
                    .filter(p -> p.toString().endsWith(".java"))
                    .sorted()
                    .collect(Collectors.toList());
        }

        Map<String, Path> classToFile = new HashMap<>();
        Set<String> allClasses = new TreeSet<>();
        Map<Path, CompilationUnit> unitsByFile = new HashMap<>();

        for (Path file : javaFiles) {
            try {
                CompilationUnit cu = StaticJavaParser.parse(file);
                unitsByFile.put(file, cu);

                cu.findAll(ClassOrInterfaceDeclaration.class).forEach(c -> {
                    if (!c.isNestedType()) {
                        allClasses.add(c.getNameAsString());
                        classToFile.put(c.getNameAsString(), file);
                    }
                });
                cu.findAll(EnumDeclaration.class).forEach(e -> {
                    if (!e.isNestedType()) {
                        allClasses.add(e.getNameAsString());
                        classToFile.put(e.getNameAsString(), file);
                    }
                });
            } catch (Exception e) {
                // Skip files that fail to parse — don't let one bad file kill the whole run
                System.out.println("=== AST SKIP: " + file.getFileName() + " — " + e.getMessage());
            }
        }

        Map<String, Set<String>> dependencySets = new TreeMap<>();
        Map<String, Set<String>> dependentSets = new TreeMap<>();
        for (String cls : allClasses) {
            dependencySets.put(cls, new TreeSet<>());
            dependentSets.put(cls, new TreeSet<>());
        }

        for (Map.Entry<Path, CompilationUnit> entry : unitsByFile.entrySet()) {
            CompilationUnit cu = entry.getValue();
            Set<String> imports = cu.getImports().stream()
                    .map(i -> i.getName().getIdentifier())
                    .collect(Collectors.toSet());

            List<String> topLevelNames = new ArrayList<>();
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(c -> {
                if (!c.isNestedType()) topLevelNames.add(c.getNameAsString());
            });
            cu.findAll(EnumDeclaration.class).forEach(e -> {
                if (!e.isNestedType()) topLevelNames.add(e.getNameAsString());
            });

            Set<String> referenced = new HashSet<>();
            referenced.addAll(imports);
            cu.findAll(ClassOrInterfaceType.class).forEach(t -> addTypeReference(t, referenced));

            for (String className : topLevelNames) {
                Set<String> deps = dependencySets.get(className);
                for (String ref : referenced) {
                    if (allClasses.contains(ref) && !ref.equals(className)) {
                        deps.add(ref);
                        dependentSets.get(ref).add(className);
                    }
                }
            }
        }

        Map<String, List<String>> dependencies = new TreeMap<>();
        Map<String, List<String>> dependents = new TreeMap<>();
        for (String cls : allClasses) {
            dependencies.put(cls, new ArrayList<>(dependencySets.get(cls)));
            dependents.put(cls, new ArrayList<>(dependentSets.get(cls)));
        }

        return new ParserAdjacency(dependencies, dependents, new ArrayList<>(allClasses));
    }

    private void addTypeReference(Type type, Set<String> refs) {
        if (type.isClassOrInterfaceType()) {
            ClassOrInterfaceType cit = type.asClassOrInterfaceType();
            refs.add(cit.getNameAsString());
            cit.getTypeArguments().ifPresent(args -> args.forEach(a -> addTypeReference(a, refs)));
        }
    }

    public void writeAdjacency(Path outputFile, ParserAdjacency adjacency) throws IOException {
        Files.createDirectories(outputFile.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputFile.toFile(), adjacency);
    }
}
package com.ripple.backend.parser;

import java.util.List;
import java.util.Map;

public class ParserAdjacency {
    private final Map<String, List<String>> dependencies;
    private final Map<String, List<String>> dependents;
    private final List<String> allClasses;

    public ParserAdjacency(
            Map<String, List<String>> dependencies,
            Map<String, List<String>> dependents,
            List<String> allClasses) {
        this.dependencies = dependencies;
        this.dependents = dependents;
        this.allClasses = allClasses;
    }

    public Map<String, List<String>> getDependencies() {
        return dependencies;
    }

    public Map<String, List<String>> getDependents() {
        return dependents;
    }

    public List<String> getAllClasses() {
        return allClasses;
    }
}

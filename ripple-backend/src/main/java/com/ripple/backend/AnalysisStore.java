package com.ripple.backend;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AnalysisStore {
    private final Map<String, RippleResponse> store = new ConcurrentHashMap<>();

    public String save(RippleResponse response) {
        String id = UUID.randomUUID().toString();
        store.put(id, response);
        return id;
    }

    public RippleResponse get(String id) {
        return store.get(id);
    }
}

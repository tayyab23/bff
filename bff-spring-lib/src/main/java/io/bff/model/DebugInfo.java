package io.bff.model;

import java.util.Map;

public class DebugInfo {
    public ResolvedRequest resolvedRequest;
    public ResolvedResponse resolvedResponse;
    public long durationMs;

    public static class ResolvedRequest {
        public String method;
        public String path;
        public Map<String, String> appliedHeaders;
        public Map<String, String> strippedHeaders;
    }

    public static class ResolvedResponse {
        public Map<String, String> headers;
    }
}

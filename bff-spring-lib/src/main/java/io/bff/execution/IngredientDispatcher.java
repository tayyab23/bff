package io.bff.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.bff.model.*;
import io.bff.registry.IngredientMetadata;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;

public class IngredientDispatcher {

    private final RequestMappingHandlerMapping handlerMapping;
    private final RequestMappingHandlerAdapter handlerAdapter;
    private final ObjectMapper objectMapper;

    public IngredientDispatcher(RequestMappingHandlerMapping handlerMapping,
                                RequestMappingHandlerAdapter handlerAdapter,
                                ObjectMapper objectMapper) {
        this.handlerMapping = handlerMapping;
        this.handlerAdapter = handlerAdapter;
        this.objectMapper = objectMapper;
    }

    public IngredientResult dispatch(IngredientMetadata meta, IngredientInput input,
                                     Map<String, IngredientResult> completed,
                                     jakarta.servlet.http.HttpServletRequest originalRequest) {
        try {
            String resolvedPath = resolvePath(meta.path(), input, completed);
            MockHttpServletRequest req = new MockHttpServletRequest(meta.httpMethod(), resolvedPath);
            req.setContentType("application/json");

            // propagate security context
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) req.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            // forward headers
            if (input.headers == null || !Boolean.FALSE.equals(input.headers.forward)) {
                originalRequest.getHeaderNames().asIterator().forEachRemaining(name ->
                    req.addHeader(name, originalRequest.getHeader(name)));
            }

            // custom headers
            if (input.headers != null && input.headers.custom != null) {
                input.headers.custom.forEach(req::addHeader);
            }

            // query params
            if (input.map != null && input.map.query != null) {
                input.map.query.forEach((k, v) -> {
                    Object resolved = FieldMapper.resolve(v, completed);
                    if (resolved != null) req.addParameter(k, resolved.toString());
                });
            }
            if (input.params != null) {
                input.params.forEach((k, v) -> req.addParameter(k, v.toString()));
            }

            // body
            if (input.body != null || (input.map != null && input.map.body != null)) {
                java.util.Map<String, Object> bodyMap = new java.util.LinkedHashMap<>();
                if (input.body instanceof Map<?,?> m) m.forEach((k, v) -> bodyMap.put(k.toString(), v));
                if (input.map != null && input.map.body != null) {
                    input.map.body.forEach((k, v) -> bodyMap.put(k, FieldMapper.resolve(v, completed)));
                }
                req.setContent(objectMapper.writeValueAsBytes(bodyMap));
            }

            MockHttpServletResponse res = new MockHttpServletResponse();
            var handler = handlerMapping.getHandler(req);
            if (handler == null) return new IngredientResult(404, Map.of("error", "Ingredient not found: " + meta.name()));
            handlerAdapter.handle(req, res, handler.getHandler());

            Object body = res.getContentAsString().isEmpty() ? null
                : objectMapper.readValue(res.getContentAsString(), Object.class);
            return new IngredientResult(res.getStatus(), body);
        } catch (Exception e) {
            return new IngredientResult(500, Map.of("error", e.getMessage()));
        }
    }

    private String resolvePath(String pathTemplate, IngredientInput input, Map<String, IngredientResult> completed) {
        String path = pathTemplate;
        if (input.map != null && input.map.path != null) {
            for (var entry : input.map.path.entrySet()) {
                Object val = FieldMapper.resolve(entry.getValue(), completed);
                if (val != null) path = path.replace("{" + entry.getKey() + "}", val.toString());
            }
        }
        if (input.params != null) {
            for (var entry : input.params.entrySet()) {
                path = path.replace("{" + entry.getKey() + "}", entry.getValue().toString());
            }
        }
        return path;
    }
}

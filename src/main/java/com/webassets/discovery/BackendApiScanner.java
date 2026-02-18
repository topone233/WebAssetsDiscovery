package com.webassets.discovery;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BackendApiScanner {
    private static final Pattern CLASS_MAPPING = Pattern.compile("@RequestMapping\\s*\\(([^)]*)\\)");
    private static final Pattern CLASS_PATH = Pattern.compile("@Path\\s*\\(([^)]*)\\)");
    private static final Pattern METHOD_DECL = Pattern.compile("(?:public|protected|private)\\s+[\\w<>,\\[\\]\\s]+\\s+(\\w+)\\s*\\(");
    private static final Pattern SPRING_METHOD_MAPPING = Pattern.compile("@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping|RequestMapping)\\s*\\(([^)]*)\\)");
    private static final Pattern JAXRS_METHOD = Pattern.compile("@(GET|POST|PUT|DELETE|PATCH)");
    private static final Pattern STRING_VALUE = Pattern.compile("\"([^\"]+)\"");
    private static final Pattern REQUEST_METHOD = Pattern.compile("RequestMethod\\.(GET|POST|PUT|DELETE|PATCH)");

    public List<ApiAsset> scan(List<Path> files, Path root) throws IOException {
        List<ApiAsset> assets = new ArrayList<>();
        for (Path file : files) {
            if (!file.toString().endsWith(".java")) {
                continue;
            }
            String content = Files.readString(file);
            assets.addAll(scanSpring(content, file, root));
            assets.addAll(scanJaxRs(content, file, root));
        }
        return deduplicate(assets);
    }

    private List<ApiAsset> scanSpring(String content, Path file, Path root) {
        List<ApiAsset> assets = new ArrayList<>();
        String classPath = extractClassLevelPath(content, CLASS_MAPPING);
        String[] lines = content.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            Matcher mapping = SPRING_METHOD_MAPPING.matcher(lines[i]);
            if (!mapping.find()) {
                continue;
            }
            String annotation = mapping.group(1);
            String args = mapping.group(2);
            String method = toSpringHttpMethod(annotation, args);
            String subPath = extractPathValue(args);
            String javaMethod = findFollowingMethodName(lines, i + 1);
            if (method == null || javaMethod == null) {
                continue;
            }
            assets.add(new ApiAsset(
                    "Spring",
                    method,
                    normalizePath(classPath, subPath),
                    javaMethod,
                    root.relativize(file).toString(),
                    i + 1
            ));
        }
        return assets;
    }

    private List<ApiAsset> scanJaxRs(String content, Path file, Path root) {
        List<ApiAsset> assets = new ArrayList<>();
        String classPath = extractClassLevelPath(content, CLASS_PATH);
        String[] lines = content.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            Matcher methodMatcher = JAXRS_METHOD.matcher(lines[i]);
            if (!methodMatcher.find()) {
                continue;
            }
            String httpMethod = methodMatcher.group(1);
            String subPath = "";
            for (int j = i + 1; j < Math.min(i + 6, lines.length); j++) {
                Matcher pathMatcher = CLASS_PATH.matcher(lines[j]);
                if (pathMatcher.find()) {
                    subPath = extractPathValue(pathMatcher.group(1));
                    break;
                }
            }
            String javaMethod = findFollowingMethodName(lines, i + 1);
            if (javaMethod == null) {
                continue;
            }
            assets.add(new ApiAsset(
                    "JAX-RS",
                    httpMethod,
                    normalizePath(classPath, subPath),
                    javaMethod,
                    root.relativize(file).toString(),
                    i + 1
            ));
        }
        return assets;
    }

    private String extractClassLevelPath(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return extractPathValue(matcher.group(1));
        }
        return "";
    }

    private String toSpringHttpMethod(String annotation, String args) {
        return switch (annotation) {
            case "GetMapping" -> "GET";
            case "PostMapping" -> "POST";
            case "PutMapping" -> "PUT";
            case "DeleteMapping" -> "DELETE";
            case "PatchMapping" -> "PATCH";
            case "RequestMapping" -> {
                Matcher matcher = REQUEST_METHOD.matcher(args);
                yield matcher.find() ? matcher.group(1) : "GET";
            }
            default -> null;
        };
    }

    private String findFollowingMethodName(String[] lines, int start) {
        for (int i = start; i < Math.min(lines.length, start + 12); i++) {
            Matcher method = METHOD_DECL.matcher(lines[i]);
            if (method.find()) {
                return method.group(1);
            }
        }
        return null;
    }

    private String extractPathValue(String annotationArgs) {
        Matcher stringMatcher = STRING_VALUE.matcher(annotationArgs);
        if (stringMatcher.find()) {
            return stringMatcher.group(1);
        }
        return "";
    }

    private String normalizePath(String classPath, String methodPath) {
        String combined = ("/" + trimSlashes(classPath) + "/" + trimSlashes(methodPath)).replaceAll("/{2,}", "/");
        if (combined.length() > 1 && combined.endsWith("/")) {
            return combined.substring(0, combined.length() - 1);
        }
        return combined;
    }

    private String trimSlashes(String s) {
        if (s == null || s.isBlank()) {
            return "";
        }
        String value = s.trim();
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private List<ApiAsset> deduplicate(List<ApiAsset> assets) {
        Map<String, ApiAsset> unique = new HashMap<>();
        for (ApiAsset api : assets) {
            unique.put(api.framework() + "|" + api.httpMethod() + "|" + api.path() + "|" + api.javaMethod() + "|" + api.sourceFile(), api);
        }
        return new ArrayList<>(unique.values());
    }
}

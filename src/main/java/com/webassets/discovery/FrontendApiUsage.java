package com.webassets.discovery;

public record FrontendApiUsage(
        String httpMethod,
        String url,
        String sourceFile,
        int line
) {
}

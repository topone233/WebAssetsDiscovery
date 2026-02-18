package com.webassets.discovery;

public record ApiAsset(
        String framework,
        String httpMethod,
        String path,
        String javaMethod,
        String sourceFile,
        int line
) {
}

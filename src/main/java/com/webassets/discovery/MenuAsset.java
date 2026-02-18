package com.webassets.discovery;

public record MenuAsset(
        String title,
        String route,
        String sourceFile,
        int line
) {
}

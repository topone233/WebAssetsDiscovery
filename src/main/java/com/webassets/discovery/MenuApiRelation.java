package com.webassets.discovery;

public record MenuApiRelation(
        String menuRoute,
        String menuTitle,
        String apiPath,
        String apiMethod,
        String confidence,
        String reason
) {
}

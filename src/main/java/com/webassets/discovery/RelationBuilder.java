package com.webassets.discovery;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RelationBuilder {
    public List<MenuApiRelation> buildRelations(List<MenuAsset> menus, List<ApiAsset> apis, List<FrontendApiUsage> usages) {
        List<MenuApiRelation> relations = new ArrayList<>();

        for (MenuAsset menu : menus) {
            for (FrontendApiUsage usage : usages) {
                if (!menu.sourceFile().equals(usage.sourceFile())) {
                    continue;
                }
                ApiAsset matchedApi = matchByPath(usage.url(), apis);
                if (matchedApi != null) {
                    relations.add(new MenuApiRelation(
                            menu.route(),
                            menu.title(),
                            matchedApi.path(),
                            matchedApi.httpMethod(),
                            "HIGH",
                            "menu与API调用出现在同一前端文件"
                    ));
                }
            }

            for (ApiAsset api : apis) {
                if (isLikelyRelated(menu.route(), api.path())) {
                    relations.add(new MenuApiRelation(
                            menu.route(),
                            menu.title(),
                            api.path(),
                            api.httpMethod(),
                            "MEDIUM",
                            "菜单路由与API路径具有相同业务前缀"
                    ));
                }
            }
        }

        return deduplicate(relations);
    }

    private ApiAsset matchByPath(String url, List<ApiAsset> apis) {
        String normalized = normalize(url);
        for (ApiAsset api : apis) {
            if (normalize(api.path()).equals(normalized) || normalize(api.path()).endsWith(normalized) || normalized.endsWith(normalize(api.path()))) {
                return api;
            }
        }
        return null;
    }

    private boolean isLikelyRelated(String route, String apiPath) {
        String r = firstSegment(route);
        String a = firstSegment(apiPath);
        return !r.isBlank() && r.equals(a);
    }

    private String firstSegment(String path) {
        String normalized = normalize(path);
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        int idx = normalized.indexOf('/');
        return idx > 0 ? normalized.substring(0, idx) : normalized;
    }

    private String normalize(String path) {
        if (path == null) {
            return "";
        }
        String normalized = path.toLowerCase(Locale.ROOT).trim().replaceAll("https?://[^/]+", "");
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized.replaceAll("/{2,}", "/");
    }

    private List<MenuApiRelation> deduplicate(List<MenuApiRelation> relations) {
        List<MenuApiRelation> deduped = new ArrayList<>();
        for (MenuApiRelation relation : relations) {
            boolean exists = deduped.stream().anyMatch(it ->
                    it.menuRoute().equals(relation.menuRoute())
                            && it.apiPath().equals(relation.apiPath())
                            && it.apiMethod().equals(relation.apiMethod()));
            if (!exists) {
                deduped.add(relation);
            }
        }
        return deduped;
    }
}

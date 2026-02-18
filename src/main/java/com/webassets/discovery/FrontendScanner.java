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

public class FrontendScanner {
    private static final Pattern MENU_ITEM_TAG = Pattern.compile("<el-menu-item[^>]*(?:index|to)=\\\"([^\\\"]+)\\\"[^>]*>([^<]+)<", Pattern.CASE_INSENSITIVE);
    private static final Pattern ROUTE_OBJECT = Pattern.compile("path\\s*:\\s*['\"]([^'\"]+)['\"][\\s\\S]{0,220}?title\\s*:\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern A_HREF = Pattern.compile("<a[^>]*href=\\\"([^\\\"]+)\\\"[^>]*>([^<]+)<", Pattern.CASE_INSENSITIVE);

    private static final Pattern AXIOS_CALL = Pattern.compile("axios\\.(get|post|put|delete|patch)\\s*\\(\\s*['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);
    private static final Pattern FETCH_CALL = Pattern.compile("fetch\\s*\\(\\s*['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTTP_CLIENT_CALL = Pattern.compile("(?:\\$http|request|http)\\.(get|post|put|delete|patch)\\s*\\(\\s*['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);

    public ScanResult scan(List<Path> files, Path root) throws IOException {
        List<MenuAsset> menus = new ArrayList<>();
        List<FrontendApiUsage> apiUsages = new ArrayList<>();

        for (Path file : files) {
            String path = file.toString();
            if (!(path.endsWith(".vue") || path.endsWith(".js") || path.endsWith(".html"))) {
                continue;
            }
            String content = Files.readString(file);
            String relativePath = root.relativize(file).toString();
            menus.addAll(scanMenus(content, relativePath));
            apiUsages.addAll(scanApiUsages(content, relativePath));
        }
        return new ScanResult(deduplicateMenus(menus), deduplicateUsages(apiUsages));
    }

    private List<MenuAsset> scanMenus(String content, String relativePath) {
        List<MenuAsset> menuAssets = new ArrayList<>();
        String[] lines = content.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            Matcher menuTag = MENU_ITEM_TAG.matcher(lines[i]);
            if (menuTag.find()) {
                menuAssets.add(new MenuAsset(menuTag.group(2).trim(), menuTag.group(1).trim(), relativePath, i + 1));
            }
            Matcher aTag = A_HREF.matcher(lines[i]);
            if (aTag.find()) {
                String href = aTag.group(1).trim();
                if (href.startsWith("/") || href.startsWith("#")) {
                    menuAssets.add(new MenuAsset(aTag.group(2).trim(), href, relativePath, i + 1));
                }
            }
        }

        Matcher routeMatcher = ROUTE_OBJECT.matcher(content);
        while (routeMatcher.find()) {
            int line = lineNumber(content, routeMatcher.start());
            menuAssets.add(new MenuAsset(routeMatcher.group(2).trim(), routeMatcher.group(1).trim(), relativePath, line));
        }
        return menuAssets;
    }

    private List<FrontendApiUsage> scanApiUsages(String content, String relativePath) {
        List<FrontendApiUsage> usages = new ArrayList<>();

        collectUsages(content, relativePath, AXIOS_CALL, usages, true);
        collectUsages(content, relativePath, HTTP_CLIENT_CALL, usages, true);

        Matcher fetch = FETCH_CALL.matcher(content);
        while (fetch.find()) {
            usages.add(new FrontendApiUsage("GET", fetch.group(1), relativePath, lineNumber(content, fetch.start())));
        }

        return usages;
    }

    private void collectUsages(String content, String relativePath, Pattern pattern,
                               List<FrontendApiUsage> usages, boolean methodInGroup1) {
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String method = methodInGroup1 ? matcher.group(1).toUpperCase() : "GET";
            String url = methodInGroup1 ? matcher.group(2) : matcher.group(1);
            usages.add(new FrontendApiUsage(method, url, relativePath, lineNumber(content, matcher.start())));
        }
    }

    private int lineNumber(String content, int position) {
        int lines = 1;
        for (int i = 0; i < position && i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                lines++;
            }
        }
        return lines;
    }

    private List<MenuAsset> deduplicateMenus(List<MenuAsset> menus) {
        Map<String, MenuAsset> unique = new HashMap<>();
        for (MenuAsset menu : menus) {
            unique.put(menu.title() + "|" + menu.route() + "|" + menu.sourceFile(), menu);
        }
        return new ArrayList<>(unique.values());
    }

    private List<FrontendApiUsage> deduplicateUsages(List<FrontendApiUsage> usages) {
        Map<String, FrontendApiUsage> unique = new HashMap<>();
        for (FrontendApiUsage usage : usages) {
            unique.put(usage.httpMethod() + "|" + usage.url() + "|" + usage.sourceFile(), usage);
        }
        return new ArrayList<>(unique.values());
    }

    public record ScanResult(List<MenuAsset> menus, List<FrontendApiUsage> apiUsages) {
    }
}

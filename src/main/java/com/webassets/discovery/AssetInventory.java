package com.webassets.discovery;

import java.util.ArrayList;
import java.util.List;

public class AssetInventory {
    private final List<ApiAsset> apis = new ArrayList<>();
    private final List<MenuAsset> menus = new ArrayList<>();
    private final List<FrontendApiUsage> frontendApiUsages = new ArrayList<>();
    private final List<MenuApiRelation> menuApiRelations = new ArrayList<>();

    public List<ApiAsset> getApis() {
        return apis;
    }

    public List<MenuAsset> getMenus() {
        return menus;
    }

    public List<FrontendApiUsage> getFrontendApiUsages() {
        return frontendApiUsages;
    }

    public List<MenuApiRelation> getMenuApiRelations() {
        return menuApiRelations;
    }
}

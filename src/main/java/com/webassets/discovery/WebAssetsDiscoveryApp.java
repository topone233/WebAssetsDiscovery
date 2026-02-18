package com.webassets.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public class WebAssetsDiscoveryApp {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("用法: java -jar web-assets-discovery.jar <源码目录> [输出文件]");
            System.exit(1);
        }

        Path sourceDir = Path.of(args[0]).toAbsolutePath().normalize();
        Path outputFile = args.length >= 2
                ? Path.of(args[1]).toAbsolutePath().normalize()
                : sourceDir.resolve("asset-inventory.json");

        List<Path> files = FileCollector.collect(sourceDir);

        BackendApiScanner backendScanner = new BackendApiScanner();
        List<ApiAsset> apis = backendScanner.scan(files, sourceDir);
        apis.sort(Comparator.comparing(ApiAsset::path).thenComparing(ApiAsset::httpMethod));

        FrontendScanner frontendScanner = new FrontendScanner();
        FrontendScanner.ScanResult frontendResult = frontendScanner.scan(files, sourceDir);

        RelationBuilder relationBuilder = new RelationBuilder();
        List<MenuApiRelation> relations = relationBuilder.buildRelations(frontendResult.menus(), apis, frontendResult.apiUsages());

        AssetInventory inventory = new AssetInventory();
        inventory.getApis().addAll(apis);
        inventory.getMenus().addAll(frontendResult.menus());
        inventory.getFrontendApiUsages().addAll(frontendResult.apiUsages());
        inventory.getMenuApiRelations().addAll(relations);

        ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.writeValue(outputFile.toFile(), inventory);

        System.out.printf("扫描完成: API=%d, 菜单=%d, 前端调用=%d, 关系=%d%n输出文件: %s%n",
                inventory.getApis().size(),
                inventory.getMenus().size(),
                inventory.getFrontendApiUsages().size(),
                inventory.getMenuApiRelations().size(),
                outputFile);
    }
}

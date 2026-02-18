# WebAssetsDiscovery

Web 应用资产发现工具（Java 实现）。

## 能力范围

针对指定源码目录扫描并输出资产清单，当前聚焦以下技术栈：

- 后端：Spring Framework（`@RequestMapping/@GetMapping...`）
- 后端：Apache CXF / JAX-RS（`@Path/@GET/@POST...`）
- 前端：Vue / HTML / JS（菜单定义、路由标题、API 调用）

输出信息包括：

- API 清单（框架、HTTP 方法、路径、Java 方法、源码位置）
- 菜单清单（标题、路由、源码位置）
- 前端 API 调用清单（HTTP 方法、URL、源码位置）
- 菜单与 API 关联关系（同文件强关联 + 路由前缀弱关联）

## 运行方式

### 1) 构建

```bash
mvn clean package
```

### 2) 扫描

```bash
java -jar target/web-assets-discovery-1.0.0.jar <源码目录> [输出文件]
```

也可以直接使用 Maven 执行：

```bash
mvn -q exec:java -Dexec.args="<源码目录> [输出文件]"
```

默认输出文件：`<源码目录>/asset-inventory.json`

## 输出示例（JSON）

```json
{
  "apis": [
    {
      "framework": "Spring",
      "httpMethod": "GET",
      "path": "/user/list",
      "javaMethod": "list",
      "sourceFile": "src/main/java/.../UserController.java",
      "line": 21
    }
  ],
  "menus": [
    {
      "title": "用户管理",
      "route": "/user",
      "sourceFile": "src/views/Menu.vue",
      "line": 15
    }
  ],
  "frontendApiUsages": [
    {
      "httpMethod": "GET",
      "url": "/user/list",
      "sourceFile": "src/views/Menu.vue",
      "line": 36
    }
  ],
  "menuApiRelations": [
    {
      "menuRoute": "/user",
      "menuTitle": "用户管理",
      "apiPath": "/user/list",
      "apiMethod": "GET",
      "confidence": "HIGH",
      "reason": "menu与API调用出现在同一前端文件"
    }
  ]
}
```

## 说明

本工具使用静态分析 + 规则匹配，适合快速盘点资产，不保证 100% 语义精确（例如动态路由拼接、复杂反射调用等场景）。

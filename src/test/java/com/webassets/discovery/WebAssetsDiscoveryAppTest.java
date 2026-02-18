package com.webassets.discovery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WebAssetsDiscoveryAppTest {

    @Test
    void shouldGenerateInventoryFromSpringCxfAndVueSources(@TempDir Path tempDir) throws Exception {
        Path javaFile = tempDir.resolve("DemoController.java");
        Files.writeString(javaFile, """
                import org.springframework.web.bind.annotation.*;
                @RestController
                @RequestMapping("/user")
                public class DemoController {
                    @GetMapping("/list")
                    public String list(){ return \"ok\"; }
                }
                """);

        Path cxfFile = tempDir.resolve("OrderService.java");
        Files.writeString(cxfFile, """
                import javax.ws.rs.*;
                @Path("/order")
                public class OrderService {
                    @POST
                    @Path("/create")
                    public String create(){ return \"ok\"; }
                }
                """);

        Path vueFile = tempDir.resolve("menu.vue");
        Files.writeString(vueFile, """
                <template>
                  <el-menu-item index=\"/user\">用户管理</el-menu-item>
                </template>
                <script>
                export default {
                  mounted(){ axios.get('/user/list'); }
                }
                </script>
                """);

        Path output = tempDir.resolve("result.json");
        WebAssetsDiscoveryApp.main(new String[]{tempDir.toString(), output.toString()});

        String json = Files.readString(output);
        assertTrue(json.contains("/user/list"));
        assertTrue(json.contains("/order/create"));
        assertTrue(json.contains("用户管理"));
    }
}

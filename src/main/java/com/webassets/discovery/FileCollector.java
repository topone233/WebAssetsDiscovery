package com.webassets.discovery;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public final class FileCollector {
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".java", ".vue", ".js", ".html");

    private FileCollector() {
    }

    public static List<Path> collect(Path root) throws IOException {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(FileCollector::isSupported)
                    .forEach(files::add);
        }
        return files;
    }

    private static boolean isSupported(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(name::endsWith);
    }
}

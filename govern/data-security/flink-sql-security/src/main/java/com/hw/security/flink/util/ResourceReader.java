package com.hw.security.flink.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Objects.requireNonNull;

/**
 * @description: ResourceReader
 * @author: HamaWhite
 */
public class ResourceReader {

    private ResourceReader() {
    }

    public static byte[] readFile(String fileName) throws URISyntaxException, IOException {
        URL url = ResourceReader.class.getClassLoader().getResource(fileName);
        Path path = Paths.get(requireNonNull(url).toURI());
        return Files.readAllBytes(path);
    }
}

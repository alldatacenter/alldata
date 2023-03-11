package com.hw.lineage.server.infrastructure.facade.impl;

import com.hw.lineage.common.exception.LineageException;
import com.hw.lineage.server.domain.facade.StorageFacade;
import com.hw.lineage.server.domain.vo.Storage;
import com.hw.lineage.server.infrastructure.config.LineageConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static java.util.Objects.requireNonNull;

/**
 * @description: StorageFacadeImpl
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Service
public class StorageFacadeImpl implements StorageFacade {

    private static final Logger LOG = LoggerFactory.getLogger(StorageFacadeImpl.class);

    private final Path rootLocation;

    public StorageFacadeImpl(LineageConfig config) {
        this.rootLocation = Paths.get(config.getStorageDir());
    }

    @Override
    public void init() throws IOException {
        if (Files.notExists(rootLocation)) {
            Files.createDirectories(rootLocation);
            LOG.info("created storageDir: {}", rootLocation.toAbsolutePath());
        }
    }

    @Override
    public String store(Storage storage, InputStream inputStream) throws IOException {
        String filePath = StringUtils.joinWith(File.separator
                , storage.getStorageType().value()
                , System.currentTimeMillis()
                , storage.getFileName()
        );

        Path destPath = buildPath(filePath);
        Files.createDirectories(destPath.getParent());

        Files.copy(inputStream, destPath, StandardCopyOption.REPLACE_EXISTING);
        return filePath;
    }

    @Override
    public void delete(String filePath) throws IOException {
        Path path = buildPath(filePath);
        Files.deleteIfExists(path);
        // check whether the parent directory is empty and delete if it is empty
        Path parent = path.getParent();
        if (requireNonNull(parent.toFile().listFiles()).length == 0) {
            Files.delete(parent);
        }
    }

    @Override
    public Resource loadAsResource(String filePath) throws MalformedURLException {
        Resource resource = new UrlResource(buildPath(filePath).toUri());
        if (resource.exists() || resource.isReadable()) {
            return resource;
        } else {
            throw new LineageException("could not read file: " + filePath);
        }
    }

    @Override
    public String getUri(String filePath) {
        return buildPath(filePath).toUri().toString();
    }

    @Override
    public String getParentUri(String filePath) {
        return buildPath(filePath).getParent().toUri().toString();
    }

    private Path buildPath(String filePath) {
        return rootLocation.resolve(filePath);
    }
}

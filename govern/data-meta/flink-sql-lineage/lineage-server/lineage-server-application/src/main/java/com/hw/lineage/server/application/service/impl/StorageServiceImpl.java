package com.hw.lineage.server.application.service.impl;

import com.hw.lineage.common.enums.StorageType;
import com.hw.lineage.server.application.service.StorageService;
import com.hw.lineage.server.domain.facade.StorageFacade;
import com.hw.lineage.server.domain.vo.Storage;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

/**
 * @description: StorageServiceImpl
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Service
public class StorageServiceImpl implements StorageService {

    @javax.annotation.Resource
    private StorageFacade storageFacade;

    @Override
    public void init() throws IOException {
        storageFacade.init();
    }

    @Override
    public String uploadFile(MultipartFile file, StorageType storageType) throws IOException {
        Storage storage = new Storage(file.getOriginalFilename(),storageType);
        // store file
        try (InputStream inputStream = file.getInputStream()) {
            return storageFacade.store(storage, inputStream);
        }
    }

    @Override
    public void deleteFile(String filePath) throws IOException {
        storageFacade.delete(filePath);
    }

    @Override
    public Resource downloadFile(String filePath) throws MalformedURLException {
        return storageFacade.loadAsResource(filePath);
    }
}

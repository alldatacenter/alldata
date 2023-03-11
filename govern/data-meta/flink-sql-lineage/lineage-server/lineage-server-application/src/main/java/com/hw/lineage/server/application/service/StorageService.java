package com.hw.lineage.server.application.service;

import com.hw.lineage.common.enums.StorageType;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;

/**
 * @description: StorageService
 * @author: HamaWhite
 * @version: 1.0.0
 */
public interface StorageService {

    void init() throws IOException;

    String uploadFile(MultipartFile file, StorageType storageType) throws IOException ;

    void deleteFile(String filePath) throws IOException;

    Resource downloadFile(String filePath) throws MalformedURLException;
}

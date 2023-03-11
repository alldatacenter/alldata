package com.hw.lineage.server.domain.facade;

import com.hw.lineage.server.domain.vo.Storage;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

/**
 * @description: StorageFacade
 * @author: HamaWhite
 * @version: 1.0.0
 */
public interface StorageFacade {
    void init() throws IOException;

    String store(Storage storage, InputStream inputStream) throws IOException ;

    void delete(String filePath) throws IOException;

     Resource loadAsResource(String filePath) throws MalformedURLException;

     String getUri(String filePath);

    String getParentUri(String filePath);
}

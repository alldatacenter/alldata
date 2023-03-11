package com.hw.lineage.server.domain.vo;

import com.google.common.io.Files;
import com.hw.lineage.common.enums.StorageType;
import lombok.Data;

import java.util.Arrays;
import java.util.List;

import static com.hw.lineage.common.util.Preconditions.checkArgument;
import static com.hw.lineage.common.util.Preconditions.checkNotNull;

/**
 * @description: Storage
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Data
public class Storage {

    protected static final List<String> SUPPORT_FILE_TYPE = Arrays.asList("jar", "xml");

    private String fileName;

    private StorageType storageType;

    public Storage(String fileName, StorageType storageType) {
        checkNotNull(fileName, "fileName cannot be null");
        String extension = Files.getFileExtension(fileName);
        checkArgument(SUPPORT_FILE_TYPE.contains(extension),
                "file type is %s, only support %s", extension, SUPPORT_FILE_TYPE);

        this.fileName = fileName;
        this.storageType = storageType;
    }
}

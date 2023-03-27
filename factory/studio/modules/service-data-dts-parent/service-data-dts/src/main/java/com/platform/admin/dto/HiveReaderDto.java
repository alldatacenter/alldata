package com.platform.admin.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 构建hive reader dto
 *
 * @author AllDataDC
 * @ClassName hive reader
 * @date 2022/01/11 17:15
 */
@Data
public class HiveReaderDto implements Serializable {

    private String readerPath;

    private String readerDefaultFS;

    private String readerFileType;

    private String readerFieldDelimiter;

    private Boolean readerSkipHeader;

}

package com.platform.admin.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 构建hive write dto
 *
 * @author AllDataDC
 * @ClassName hive write dto
 * @date 2022/01/11 17:15
 */
@Data
public class HiveWriterDto implements Serializable {

    private String writerDefaultFS;

    private String writerFileType;

    private String writerPath;

    private String writerFileName;

    private String writeMode;

    private String writeFieldDelimiter;
}

package com.platform.admin.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 构建json dto
 *
 * @author AllDataDC
 * @ClassName FlinkxJsonDto
 * @Version 2.1.1
 * @date 2022/03/14 07:15
 */
@Data
public class FlinkXJsonBuildDto implements Serializable {

    private Long readerDatasourceId;

    private List<String> readerTables;

    private List<String> readerColumns;

    private Long writerDatasourceId;

    private List<String> writerTables;

    private List<String> writerColumns;

    private HiveReaderDto hiveReader;

    private HiveWriterDto hiveWriter;

    private HbaseReaderDto hbaseReader;

    private HbaseWriterDto hbaseWriter;

    private RdbmsReaderDto rdbmsReader;

    private RdbmsWriterDto rdbmsWriter;

    private MongoDBReaderDto mongoDBReader;

    private MongoDBWriterDto mongoDBWriter;

    private ClickhouseReaderDto clickhouseReader;

    private ClickhouseWriterDto clickhouseWriter;
}

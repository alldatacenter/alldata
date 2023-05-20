package com.platform.admin.tool.flinkx;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.platform.admin.dto.*;
import com.platform.admin.entity.JobDatasource;
import com.platform.admin.tool.flinkx.reader.*;
import com.platform.admin.tool.flinkx.writer.*;
import com.platform.admin.tool.pojo.FlinkxHbasePojo;
import com.platform.admin.tool.pojo.FlinkxHivePojo;
import com.platform.admin.tool.pojo.FlinkxMongoDBPojo;
import com.platform.admin.tool.pojo.FlinkxRdbmsPojo;
import com.platform.admin.util.JdbcConstants;
import com.platform.core.util.Constants;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JSON的构建类
 */
@Data
public class FlinkxJsonHelper implements FlinkxJsonInterface {

    /**
     * 读取的表，根据flinkx示例，支持多个表（先不考虑，后面再去实现， 这里先用list保存吧）
     * <p>
     * 目的表的表名称。支持写入一个或者多个表。当配置为多张表时，必须确保所有表结构保持一致
     */
    private List<String> readerTables;
    /**
     * 读取的字段列表
     */
    private List<String> readerColumns;
    /**
     * reader jdbc 数据源
     */
    private JobDatasource readerDatasource;
    /**
     * writer jdbc 数据源
     */
    private JobDatasource writerDatasource;
    /**
     * 写入的表
     */
    private List<String> writerTables;
    /**
     * 写入的字段列表
     */
    private List<String> writerColumns;

    private Map<String, Object> buildReader;

    private Map<String, Object> buildWriter;

    private BaseFlinkxPlugin readerPlugin;

    private BaseFlinkxPlugin writerPlugin;

    private HiveReaderDto hiveReaderDto;

    private HiveWriterDto hiveWriterDto;

    private HbaseReaderDto hbaseReaderDto;

    private HbaseWriterDto hbaseWriterDto;

    private RdbmsReaderDto rdbmsReaderDto;

    private RdbmsWriterDto rdbmsWriterDto;

    private MongoDBReaderDto mongoDBReaderDto;

    private MongoDBWriterDto mongoDBWriterDto;

    private ClickhouseReaderDto clickhouseReaderDto;

    private ClickhouseWriterDto clickhouseWriterDto;


    //用于保存额外参数
    private Map<String, Object> extraParams = Maps.newHashMap();

    public void initReader(FlinkXJsonBuildDto flinkxJsonDto, JobDatasource readerDatasource) {

        this.readerDatasource = readerDatasource;
        this.readerTables = flinkxJsonDto.getReaderTables();
        this.readerColumns = flinkxJsonDto.getReaderColumns();
        this.hiveReaderDto = flinkxJsonDto.getHiveReader();
        this.rdbmsReaderDto = flinkxJsonDto.getRdbmsReader();
        this.hbaseReaderDto = flinkxJsonDto.getHbaseReader();
        this.clickhouseReaderDto = flinkxJsonDto.getClickhouseReader();
        // reader 插件
        String datasource = readerDatasource.getDatasource();

//        this.readerColumns = convertKeywordsColumns(datasource, this.readerColumns);
        if (JdbcConstants.MYSQL.equals(datasource)) {
            readerPlugin = new MysqlReader();
            buildReader = buildReader();
        } else if (JdbcConstants.ORACLE.equals(datasource)) {
            readerPlugin = new OracleReader();
            buildReader = buildReader();
        } else if (JdbcConstants.HANA.equals(datasource)) {
            readerPlugin = new HanaReader();
            buildReader = buildReader();
        } else if (JdbcConstants.SQL_SERVER.equals(datasource)) {
            readerPlugin = new SqlServerReader();
            buildReader = buildReader();
        } else if (JdbcConstants.POSTGRESQL.equals(datasource)) {
            readerPlugin = new PostgresqlReader();
            buildReader = buildReader();
        } else if (JdbcConstants.CLICKHOUSE.equals(datasource)) {
            readerPlugin = new ClickHouseReader();
            buildReader = buildReader();
        } else if (JdbcConstants.HIVE.equals(datasource)) {
            readerPlugin = new HiveReader();
            buildReader = buildHiveReader();
        } else if (JdbcConstants.HBASE.equals(datasource)) {
            readerPlugin = new HBaseReader();
            buildReader = buildHBaseReader();
        } else if (JdbcConstants.MONGODB.equals(datasource)) {
            readerPlugin = new MongoDBReader();
            buildReader = buildMongoDBReader();
        }
    }

    public void initWriter(FlinkXJsonBuildDto flinkxJsonDto, JobDatasource readerDatasource) {
        this.writerDatasource = readerDatasource;
        this.writerTables = flinkxJsonDto.getWriterTables();
        this.writerColumns = flinkxJsonDto.getWriterColumns();
        this.hiveWriterDto = flinkxJsonDto.getHiveWriter();
        this.rdbmsWriterDto = flinkxJsonDto.getRdbmsWriter();
        this.hbaseWriterDto = flinkxJsonDto.getHbaseWriter();
        this.mongoDBWriterDto = flinkxJsonDto.getMongoDBWriter();
        // writer
        String datasource = readerDatasource.getDatasource();
//        this.writerColumns = convertKeywordsColumns(datasource, this.writerColumns);
        if (JdbcConstants.MYSQL.equals(datasource)) {
            writerPlugin = new MysqlWriter();
            buildWriter = this.buildWriter();
        } else if (JdbcConstants.ORACLE.equals(datasource)) {
            writerPlugin = new OraclelWriter();
            buildWriter = this.buildWriter();
        } else if (JdbcConstants.HANA.equals(datasource)) {
            writerPlugin = new HanaWriter();
            buildWriter = this.buildWriter();
        } else if (JdbcConstants.SQL_SERVER.equals(datasource)) {
            writerPlugin = new SqlServerlWriter();
            buildWriter = this.buildWriter();
        } else if (JdbcConstants.POSTGRESQL.equals(datasource)) {
            writerPlugin = new PostgresqllWriter();
            buildWriter = this.buildWriter();
        } else if (JdbcConstants.CLICKHOUSE.equals(datasource)) {
            writerPlugin = new ClickHouseWriter();
            buildWriter = buildWriter();
        } else if (JdbcConstants.HIVE.equals(datasource)) {
            writerPlugin = new HiveWriter();
            buildWriter = this.buildHiveWriter();
        } else if (JdbcConstants.HBASE.equals(datasource)) {
            writerPlugin = new HBaseWriter();
            buildWriter = this.buildHBaseWriter();
        } else if (JdbcConstants.MONGODB.equals(datasource)) {
            writerPlugin = new MongoDBWriter();
            buildWriter = this.buildMongoDBWriter();
        }
    }

    private List<String> convertKeywordsColumns(String datasource, List<String> columns) {
        if (columns == null) {
            return null;
        }

        List<String> toColumns = new ArrayList<>();
        columns.forEach(s -> {
            toColumns.add(doConvertKeywordsColumn(datasource, s));
        });
        return toColumns;
    }

    private String doConvertKeywordsColumn(String dbType, String column) {
        if (column == null) {
            return null;
        }

        column = column.trim();
        column = column.replace("[", "");
        column = column.replace("]", "");
        column = column.replace("`", "");
        column = column.replace("\"", "");
        column = column.replace("'", "");

        switch (dbType) {
            case JdbcConstants.MYSQL:
                return String.format("`%s`", column);
            case JdbcConstants.SQL_SERVER:
                return String.format("[%s]", column);
            case JdbcConstants.POSTGRESQL:
            case JdbcConstants.ORACLE:
                return String.format("\"%s\"", column);
            case JdbcConstants.HANA:
                return String.format("\"%s\"", column);
            default:
                return column;
        }
    }

    @Override
    public Map<String, Object> buildJob() {
        Map<String, Object> res = Maps.newLinkedHashMap();
        Map<String, Object> jobMap = Maps.newLinkedHashMap();
        jobMap.put("setting", buildSetting());
        jobMap.put("content", ImmutableList.of(buildContent()));
        res.put("job", jobMap);
        return res;
    }

    @Override
    public Map<String, Object> buildSetting() {
        Map<String, Object> res = Maps.newLinkedHashMap();
        Map<String, Object> speedMap = Maps.newLinkedHashMap();
        Map<String, Object> errorLimitMap = Maps.newLinkedHashMap();

        Map<String, Object> restoreMap = Maps.newLinkedHashMap();
        Map<String, Object> logMap = Maps.newLinkedHashMap();
        speedMap.putAll(ImmutableMap.of("channel", 1, "bytes", 0));
        errorLimitMap.putAll(ImmutableMap.of("record", 100));
        restoreMap.putAll(ImmutableMap.of("maxRowNumForCheckpoint", 0,"isRestore",false,"restoreColumnName","","restoreColumnIndex",0));
        logMap.putAll(ImmutableMap.of("isLogger", false,"level","debug","path","","pattern",""));
        res.put("speed", speedMap);
        res.put("errorLimit", errorLimitMap);
        res.put("restore",restoreMap);
        res.put("log",logMap);
        return res;
    }

    @Override
    public Map<String, Object> buildContent() {
        Map<String, Object> res = Maps.newLinkedHashMap();
        res.put("reader", this.buildReader);
        res.put("writer", this.buildWriter);
        return res;
    }

	@Override
	public Map<String, Object> buildReader() {
		FlinkxRdbmsPojo flinkxPluginPojo = new FlinkxRdbmsPojo();
		flinkxPluginPojo.setJobDatasource(readerDatasource);
		flinkxPluginPojo.setTables(readerTables);
//		List<Map<String, Object>> columns = Lists.newArrayList();
//		readerColumns.forEach(c -> {
//			Map<String, Object> column = Maps.newLinkedHashMap();
//			column.put("name", c.split(Constants.SPLIT_SCOLON)[0]);
//			//			column.put("index", Integer.parseInt(c.split(Constants.SPLIT_SCOLON)[0]));
//			//			column.put("type", c.split(Constants.SPLIT_SCOLON)[2]);
//			columns.add(column);
//		});
		flinkxPluginPojo.setRdbmsColumns(readerColumns);
		flinkxPluginPojo.setSplitPk(rdbmsReaderDto.getReaderSplitPk());
		if (StringUtils.isNotBlank(rdbmsReaderDto.getQuerySql())) {
			flinkxPluginPojo.setQuerySql(rdbmsReaderDto.getQuerySql());
		}
		//where
		if (StringUtils.isNotBlank(rdbmsReaderDto.getWhereParams())) {
			flinkxPluginPojo.setWhereParam(rdbmsReaderDto.getWhereParams());
		}
		return readerPlugin.build(flinkxPluginPojo);
	}

    @Override
    public Map<String, Object> buildHiveReader() {
        FlinkxHivePojo flinkxHivePojo = new FlinkxHivePojo();
        flinkxHivePojo.setJdbcDatasource(readerDatasource);
        List<Map<String, Object>> columns = Lists.newArrayList();
        readerColumns.forEach(c -> {
            Map<String, Object> column = Maps.newLinkedHashMap();
            column.put("name", c.split(Constants.SPLIT_SCOLON)[1]);
            column.put("index", Integer.parseInt(c.split(Constants.SPLIT_SCOLON)[0]));
            column.put("type", c.split(Constants.SPLIT_SCOLON)[2]);
            columns.add(column);
        });
        flinkxHivePojo.setColumns(columns);
        flinkxHivePojo.setReaderDefaultFS(hiveReaderDto.getReaderDefaultFS());
        flinkxHivePojo.setReaderFieldDelimiter(hiveReaderDto.getReaderFieldDelimiter());
        flinkxHivePojo.setReaderFileType(hiveReaderDto.getReaderFileType());
        flinkxHivePojo.setReaderPath(hiveReaderDto.getReaderPath());
        flinkxHivePojo.setSkipHeader(hiveReaderDto.getReaderSkipHeader());
        return readerPlugin.buildHive(flinkxHivePojo);
    }

    @Override
    public Map<String, Object> buildHBaseReader() {
        FlinkxHbasePojo flinkxHbasePojo = new FlinkxHbasePojo();
        flinkxHbasePojo.setJdbcDatasource(readerDatasource);
        List<Map<String, Object>> columns = Lists.newArrayList();
        for (int i = 0; i < readerColumns.size(); i++) {
            Map<String, Object> column = Maps.newLinkedHashMap();
            column.put("name", readerColumns.get(i));
            column.put("type", "string");
            columns.add(column);
        }
        flinkxHbasePojo.setColumns(columns);
        flinkxHbasePojo.setReaderHbaseConfig(readerDatasource.getZkAdress());
        String readerTable=!CollectionUtils.isEmpty(readerTables)?readerTables.get(0):Constants.STRING_BLANK;
        flinkxHbasePojo.setReaderTable(readerTable);
        flinkxHbasePojo.setReaderMode(hbaseReaderDto.getReaderMode());
        flinkxHbasePojo.setReaderRange(hbaseReaderDto.getReaderRange());
        return readerPlugin.buildHbase(flinkxHbasePojo);
    }

    @Override
    public Map<String, Object> buildMongoDBReader() {
        FlinkxMongoDBPojo flinkxMongoDBPojo = new FlinkxMongoDBPojo();
        flinkxMongoDBPojo.setJdbcDatasource(readerDatasource);
        List<Map<String, Object>> columns = Lists.newArrayList();
        buildColumns(readerColumns, columns);
        flinkxMongoDBPojo.setColumns(columns);
        flinkxMongoDBPojo.setAddress(readerDatasource.getJdbcUrl());
        flinkxMongoDBPojo.setDbName(readerDatasource.getDatabaseName());
        flinkxMongoDBPojo.setReaderTable(readerTables.get(0));
        return readerPlugin.buildMongoDB(flinkxMongoDBPojo);
    }


	@Override
	public Map<String, Object> buildWriter() {
		FlinkxRdbmsPojo flinkxPluginPojo = new FlinkxRdbmsPojo();
		flinkxPluginPojo.setJobDatasource(writerDatasource);
		flinkxPluginPojo.setTables(writerTables);
//		List<Map<String, Object>> columns = Lists.newArrayList();
//		writerColumns.forEach(c -> {
//			Map<String, Object> column = Maps.newLinkedHashMap();
//			column.put("name", c.split(Constants.SPLIT_SCOLON)[0]);
//			//			column.put("type", c.split(Constants.SPLIT_SCOLON)[2]);
//			columns.add(column);
//		});
		flinkxPluginPojo.setRdbmsColumns(writerColumns);
		flinkxPluginPojo.setPreSql(rdbmsWriterDto.getPreSql());
		flinkxPluginPojo.setPostSql(rdbmsWriterDto.getPostSql());
		return writerPlugin.build(flinkxPluginPojo);
	}

    @Override
    public Map<String, Object> buildHiveWriter() {
        FlinkxHivePojo flinkxHivePojo = new FlinkxHivePojo();
        flinkxHivePojo.setJdbcDatasource(writerDatasource);
        List<Map<String, Object>> columns = Lists.newArrayList();
        writerColumns.forEach(c -> {
            Map<String, Object> column = Maps.newLinkedHashMap();
            column.put("name", c.split(Constants.SPLIT_SCOLON)[1]);
            column.put("type", c.split(Constants.SPLIT_SCOLON)[2]);
            columns.add(column);
        });
        flinkxHivePojo.setColumns(columns);
        flinkxHivePojo.setWriterDefaultFS(hiveWriterDto.getWriterDefaultFS());
        flinkxHivePojo.setWriteFieldDelimiter(hiveWriterDto.getWriteFieldDelimiter());
        flinkxHivePojo.setWriterFileType(hiveWriterDto.getWriterFileType());
        flinkxHivePojo.setWriterPath(hiveWriterDto.getWriterPath());
        flinkxHivePojo.setWriteMode(hiveWriterDto.getWriteMode());
        flinkxHivePojo.setWriterFileName(hiveWriterDto.getWriterFileName());
        return writerPlugin.buildHive(flinkxHivePojo);
    }

    @Override
    public Map<String, Object> buildHBaseWriter() {
        FlinkxHbasePojo flinkxHbasePojo = new FlinkxHbasePojo();
        flinkxHbasePojo.setJdbcDatasource(writerDatasource);
        List<Map<String, Object>> columns = Lists.newArrayList();
        for (int i = 0; i < writerColumns.size(); i++) {
            Map<String, Object> column = Maps.newLinkedHashMap();
            column.put("index", i + 1);
            column.put("name", writerColumns.get(i));
            column.put("type", "string");
            columns.add(column);
        }
        flinkxHbasePojo.setColumns(columns);
        flinkxHbasePojo.setWriterHbaseConfig(writerDatasource.getZkAdress());
        String writerTable=!CollectionUtils.isEmpty(writerTables)?writerTables.get(0):Constants.STRING_BLANK;
        flinkxHbasePojo.setWriterTable(writerTable);
        flinkxHbasePojo.setWriterVersionColumn(hbaseWriterDto.getWriterVersionColumn());
        flinkxHbasePojo.setWriterRowkeyColumn(hbaseWriterDto.getWriterRowkeyColumn());
        flinkxHbasePojo.setWriterMode(hbaseWriterDto.getWriterMode());
        return writerPlugin.buildHbase(flinkxHbasePojo);
    }


    @Override
    public Map<String, Object> buildMongoDBWriter() {
        FlinkxMongoDBPojo flinkxMongoDBPojo = new FlinkxMongoDBPojo();
        flinkxMongoDBPojo.setJdbcDatasource(writerDatasource);
        List<Map<String, Object>> columns = Lists.newArrayList();
        buildColumns(writerColumns, columns);
        flinkxMongoDBPojo.setColumns(columns);
        flinkxMongoDBPojo.setAddress(writerDatasource.getJdbcUrl());
        flinkxMongoDBPojo.setDbName(writerDatasource.getDatabaseName());
        flinkxMongoDBPojo.setWriterTable(writerTables.get(0));
        flinkxMongoDBPojo.setUpsertInfo(mongoDBWriterDto.getUpsertInfo());
        return writerPlugin.buildMongoDB(flinkxMongoDBPojo);
    }

    private void buildColumns(List<String> columns, List<Map<String, Object>> returnColumns) {
        columns.forEach(c -> {
            Map<String, Object> column = Maps.newLinkedHashMap();
            column.put("name", c.split(Constants.SPLIT_SCOLON)[0]);
            column.put("type", c.split(Constants.SPLIT_SCOLON)[1]);
            returnColumns.add(column);
        });
    }
}

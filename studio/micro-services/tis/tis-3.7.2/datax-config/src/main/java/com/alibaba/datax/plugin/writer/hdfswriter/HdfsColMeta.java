package com.alibaba.datax.plugin.writer.hdfswriter;

import com.alibaba.datax.common.util.Configuration;
import com.qlangtech.tis.plugin.ds.DataType;
import com.qlangtech.tis.plugin.ds.IColMetaGetter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-02-18 15:47
 **/
public class HdfsColMeta implements Serializable, IColMetaGetter {

    public static final String KEY_COLUMN = "column";
    public static final String KEY_NAME = "name";
    public static final String KEY_TYPE = "type";
    public static final String KEY_NULLABLE = "nullable";
    public static final String KEY_PK = "pk";

    public final String colName;
    public final boolean nullable;
    public final DataType type;
    public final CsvType csvType;
    public final boolean pk;

    public static <T extends IColMetaGetter> List<T> getColsMeta(Configuration config) {
        Objects.requireNonNull(config, "param config can not be null");
        List<T> result = new ArrayList<>();
        List<Configuration> columns = config.getListConfiguration(KEY_COLUMN);
        DataType hiveType = null;
        T meta = null;
        for (Configuration cfg : columns) {
            hiveType = DataType.ds(cfg.getString(KEY_TYPE));
            meta = (T) (new HdfsColMeta(cfg.getString(KEY_NAME)
                    , cfg.getBool(KEY_NULLABLE, true)
                    , cfg.getBool(KEY_PK, false)
                    , hiveType));
            result.add(meta);
        }
        return result;
    }

    public HdfsColMeta(String colName, Boolean nullable, Boolean pk, DataType dataType) {
        this.colName = colName;
        if (nullable == null) {
            throw new IllegalStateException("config of col:" + colName + " relevant nullable prop can not be null");
        }
        this.nullable = nullable;
        this.type = dataType;
        this.csvType = parseCsvType(dataType);
        this.pk = pk;
    }

    @Override
    public boolean isPk() {
        return this.pk;
    }

    @Override
    public DataType getType() {
        return this.type;
    }

    public String getName() {
        return this.colName;
    }

    private static CsvType parseCsvType(DataType dataType) {
        switch (dataType.getCollapse()) {
            case STRING:
            case Date:
            case Bytes:
                return CsvType.STRING;
            case Long:
            case INT:
            case Double:
                return CsvType.NUMBER;
            case Boolean:
                return CsvType.BOOLEAN;
            default:
                throw new IllegalStateException("illegal type:" + dataType.getCollapse());
        }


//        switch (hiveType) {
//            case DATE:
//            case CHAR:
//            case STRING:
//            case VARCHAR:
//            case TIMESTAMP:
//                return CsvType.STRING;
//            case TINYINT:
//            case INT:
//            case FLOAT:
//            case BIGINT:
//            case DOUBLE:
//            case SMALLINT:
//                return CsvType.NUMBER;
//            case BOOLEAN:
//                return CsvType.BOOLEAN;
//            default:
//                throw new IllegalStateException(" relevant type:" + hiveType + " is illegal");
//        }
    }

    public enum CsvType {
        STRING,
        NUMBER,
        BOOLEAN
    }
}

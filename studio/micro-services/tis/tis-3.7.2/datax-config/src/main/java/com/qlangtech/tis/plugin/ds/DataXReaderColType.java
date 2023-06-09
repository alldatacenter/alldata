package com.qlangtech.tis.plugin.ds;

import org.apache.commons.lang3.StringUtils;

import java.sql.Types;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * https://github.com/alibaba/DataX/blob/master/mysqlreader/doc/mysqlreader.md#33-%E7%B1%BB%E5%9E%8B%E8%BD%AC%E6%8D%A2
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-02-19 09:12
 **/
public enum DataXReaderColType {
    Long("long", new DataType(Types.BIGINT)),
    INT("int", new DataType(Types.INTEGER)),
    Double("double", new DataType(Types.DOUBLE)),
    STRING("string", DataType.createVarChar(256)),
    Boolean("boolean", new DataType(Types.BOOLEAN)),
    Date("date", new DataType(Types.DATE)),
    Bytes("bytes", new DataType(Types.BLOB));

    private final String literia;
    public final DataType dataType;

    private DataXReaderColType(String literia, DataType dataType) {
        this.literia = literia;
        this.dataType = dataType;
    }

    public static DataType parse(String literia) {
        literia = StringUtils.lowerCase(literia);
        for (DataXReaderColType t : DataXReaderColType.values()) {
            if (literia.equals(t.literia)) {
                return t.dataType;
            }
        }
        return null;
    }

    public String getLiteria() {
        return literia;
    }

//        /**
//         * https://github.com/alibaba/DataX/blob/master/mysqlreader/doc/mysqlreader.md#33-%E7%B1%BB%E5%9E%8B%E8%BD%AC%E6%8D%A2
//         *
//         * @param mysqlType java.sql.Types
//         * @return
//         */
//        public static ColumnMetaData.DataType parse(int mysqlType) {
//            return new ColumnMetaData.DataType(mysqlType);
//            switch (mysqlType) {
//                case Types.INTEGER:
//                case Types.TINYINT:
//                case Types.SMALLINT:
//                case Types.BIGINT:
//                    return new ColumnMetaData.DataType( DataXReaderColType.Long;
//                case Types.FLOAT:
//                case Types.DOUBLE:
//                case Types.DECIMAL:
//                    return DataXReaderColType.Double;
//                case Types.DATE:
//                case Types.TIME:
//                case Types.TIMESTAMP:
//                    return DataXReaderColType.Date;
//                case Types.BIT:
//                case Types.BOOLEAN:
//                    return DataXReaderColType.Boolean;
//                case Types.BLOB:
//                case Types.BINARY:
//                case Types.LONGVARBINARY:
//                case Types.VARBINARY:
//                    return DataXReaderColType.Bytes;
//                default:
//                    return DataXReaderColType.STRING;
//            }
//        }

    @Override
    public String toString() {
        return this.literia;
    }

    public static String toDesc() {
        return Arrays.stream(DataXReaderColType.values()).map((t) -> "'" + t.literia + "'").collect(Collectors.joining(","));
    }
}

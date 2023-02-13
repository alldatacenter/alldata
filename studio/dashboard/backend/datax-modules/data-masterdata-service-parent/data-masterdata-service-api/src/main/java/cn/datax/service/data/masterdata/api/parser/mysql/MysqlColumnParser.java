package cn.datax.service.data.masterdata.api.parser.mysql;

import cn.datax.common.exception.DataException;
import cn.datax.service.data.masterdata.api.enums.MysqlDataTypeEnum;
import cn.datax.service.data.masterdata.api.enums.OracleDataTypeEnum;
import cn.datax.service.data.masterdata.api.parser.ColumnParser;
import cn.datax.service.data.masterdata.api.parser.DataType;

public class MysqlColumnParser extends ColumnParser {

    @Override
    public DataType oracleParse(OracleDataTypeEnum dataTypeEnum) {
        return null;
    }

    @Override
    public DataType mysqlParse(MysqlDataTypeEnum dataTypeEnum) {
        switch(dataTypeEnum) {
            case TINYINT:
                return new MysqlTinyintDataType();
            case INT:
                return new MysqlIntDataType();
            case BIGINT:
                return new MysqlBigintDataType();
            case FLOAT:
                return new MysqlFloatDataType();
            case DOUBLE:
                return new MysqlDoubleDataType();
            case DECIMAL:
                return new MysqlDecimalDataType();
            case CHAR:
                return new MysqlCharDataType();
            case VARCHAR:
                return new MysqlVarcharDataType();
            case TEXT:
                return new MysqlTextDataType();
            case DATE:
                return new MysqlDateDataType();
            case TIME:
                return new MysqlTimeDataType();
            case YEAR:
                return new MysqlYearDataType();
            case DATETIME:
                return new MysqlDatetimeDataType();
            case BLOB:
                return new MysqlBlobDataType();
            default:
                throw new DataException("字段数据类型错误");
        }
    }
}

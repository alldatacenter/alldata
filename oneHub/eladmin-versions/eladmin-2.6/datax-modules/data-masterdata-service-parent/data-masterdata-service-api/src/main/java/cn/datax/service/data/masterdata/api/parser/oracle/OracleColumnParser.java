package cn.datax.service.data.masterdata.api.parser.oracle;

import cn.datax.common.exception.DataException;
import cn.datax.service.data.masterdata.api.enums.MysqlDataTypeEnum;
import cn.datax.service.data.masterdata.api.enums.OracleDataTypeEnum;
import cn.datax.service.data.masterdata.api.parser.ColumnParser;
import cn.datax.service.data.masterdata.api.parser.DataType;

public class OracleColumnParser extends ColumnParser {

    @Override
    public DataType oracleParse(OracleDataTypeEnum dataTypeEnum) {
        switch(dataTypeEnum) {
            case CHAR:
                return new OracleCharDataType();
            case DATE:
                return new OracleDateDataType();
            case NUMBER:
                return new OracleNumberDataType();
            case CLOB:
                return new OracleClobDataType();
            case BLOB:
                return new OracleBlobDataType();
            default:
                throw new DataException("字段数据类型错误");
        }
    }

    @Override
    public DataType mysqlParse(MysqlDataTypeEnum dataTypeEnum)  {
        return null;
    }
}

package cn.datax.service.data.masterdata.api.parser;

import cn.datax.service.data.masterdata.api.enums.MysqlDataTypeEnum;
import cn.datax.service.data.masterdata.api.enums.OracleDataTypeEnum;

public abstract class ColumnParser {

	public abstract DataType oracleParse(OracleDataTypeEnum dataTypeEnum);

	public abstract DataType mysqlParse(MysqlDataTypeEnum dataTypeEnum);
}

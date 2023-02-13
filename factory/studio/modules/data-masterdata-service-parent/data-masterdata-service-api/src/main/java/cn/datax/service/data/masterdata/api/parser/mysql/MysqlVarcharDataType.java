package cn.datax.service.data.masterdata.api.parser.mysql;

import cn.datax.service.data.masterdata.api.parser.DataType;

import java.util.Optional;

public class MysqlVarcharDataType implements DataType {

    @Override
    public String fillTypeString(String columnLength, String columnScale) {
        return "varchar(" + Optional.ofNullable(columnLength).orElse("255") + ")";
    }
}

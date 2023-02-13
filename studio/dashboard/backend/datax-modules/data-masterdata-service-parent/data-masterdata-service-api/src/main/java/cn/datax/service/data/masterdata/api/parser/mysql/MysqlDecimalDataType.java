package cn.datax.service.data.masterdata.api.parser.mysql;

import cn.datax.service.data.masterdata.api.parser.DataType;

import java.util.Optional;

public class MysqlDecimalDataType implements DataType {

    @Override
    public String fillTypeString(String columnLength, String columnScale) {
        return "decimal(" + Optional.ofNullable(columnLength).orElse("10") + ", " + Optional.ofNullable(columnScale).orElse("2") + ")";
    }
}

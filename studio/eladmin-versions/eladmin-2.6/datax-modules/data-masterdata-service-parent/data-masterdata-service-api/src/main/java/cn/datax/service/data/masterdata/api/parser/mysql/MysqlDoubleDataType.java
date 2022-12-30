package cn.datax.service.data.masterdata.api.parser.mysql;

import cn.datax.service.data.masterdata.api.parser.DataType;

import java.util.Optional;

public class MysqlDoubleDataType implements DataType {

    @Override
    public String fillTypeString(String columnLength, String columnScale) {
        return "double(" + Optional.ofNullable(columnLength).orElse("5") + ", " + Optional.ofNullable(columnScale).orElse("2") + ")";
    }
}

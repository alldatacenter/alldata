package cn.datax.service.data.masterdata.api.parser.mysql;

import cn.datax.service.data.masterdata.api.parser.DataType;

import java.util.Optional;

public class MysqlFloatDataType implements DataType {

    @Override
    public String fillTypeString(String columnLength, String columnScale) {
        return "float(" + Optional.ofNullable(columnLength).orElse("5") + ", " + Optional.ofNullable(columnScale).orElse("2") + ")";
    }
}

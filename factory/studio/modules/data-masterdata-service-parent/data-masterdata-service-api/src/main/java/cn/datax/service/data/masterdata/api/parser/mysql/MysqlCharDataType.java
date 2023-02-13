package cn.datax.service.data.masterdata.api.parser.mysql;

import cn.datax.service.data.masterdata.api.parser.DataType;

import java.util.Optional;

public class MysqlCharDataType implements DataType {

    @Override
    public String fillTypeString(String columnLength, String columnScale) {
        return "char(" + Optional.ofNullable(columnLength).orElse("1") + ")";
    }
}

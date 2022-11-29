package cn.datax.service.data.masterdata.api.parser.oracle;

import cn.datax.service.data.masterdata.api.parser.DataType;

import java.util.Optional;

public class OracleCharDataType implements DataType {

    @Override
    public String fillTypeString(String columnLength, String columnScale) {
        return "VARCHAR2(" + Optional.ofNullable(columnLength).orElse("255") + ")";
    }
}

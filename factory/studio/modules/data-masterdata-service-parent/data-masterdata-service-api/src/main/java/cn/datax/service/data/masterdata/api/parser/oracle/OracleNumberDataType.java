package cn.datax.service.data.masterdata.api.parser.oracle;

import cn.datax.service.data.masterdata.api.parser.DataType;

import java.util.Optional;

public class OracleNumberDataType implements DataType {

    @Override
    public String fillTypeString(String columnLength, String columnScale) {
        return "NUMBER(" + Optional.ofNullable(columnLength).orElse("22") + ", " + Optional.ofNullable(columnScale).orElse("0") + ")";
    }
}

package cn.datax.service.data.masterdata.api.parser.oracle;

import cn.datax.service.data.masterdata.api.parser.DataType;

public class OracleDateDataType implements DataType {

    @Override
    public String fillTypeString(String columnLength, String columnScale) {
        return "DATE";
    }
}

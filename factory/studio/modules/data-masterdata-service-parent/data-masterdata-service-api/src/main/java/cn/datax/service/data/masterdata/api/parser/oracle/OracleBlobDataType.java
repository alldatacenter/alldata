package cn.datax.service.data.masterdata.api.parser.oracle;

import cn.datax.service.data.masterdata.api.parser.DataType;

public class OracleBlobDataType implements DataType {

    @Override
    public String fillTypeString(String columnLength, String columnScale) {
        return "BLOB";
    }
}

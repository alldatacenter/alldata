package cn.datax.service.data.masterdata.api.parser.mysql;

import cn.datax.service.data.masterdata.api.parser.DataType;

public class MysqlTextDataType implements DataType {

    @Override
    public String fillTypeString(String columnLength, String columnScale) {
        return "text";
    }
}

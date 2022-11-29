package cn.datax.service.data.masterdata.api.parser.mysql;

import cn.datax.service.data.masterdata.api.parser.DataType;

public class MysqlTimeDataType implements DataType {

    @Override
    public String fillTypeString(String columnLength, String columnScale) {
        return "time";
    }
}

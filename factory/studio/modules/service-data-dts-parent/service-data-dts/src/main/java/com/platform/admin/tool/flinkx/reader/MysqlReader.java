package com.platform.admin.tool.flinkx.reader;


import java.util.Map;

/**
 * mysql reader 构建类
 *
 * @author AllDataDC
 * @ClassName MysqlReader * @date 2022/7/30 23:07
 */
public class MysqlReader extends BaseReaderPlugin implements FlinkxReaderInterface {
    @Override
    public String getName() {
        return "mysqlreader";
    }


    @Override
    public Map<String, Object> sample() {
        return null;
    }
}

package com.platform.admin.tool.flinkx.writer;

import java.util.Map;

/**
 * mysql writer构建类
 *
 * @author AllDataDC
 * @ClassName MysqlWriter * @date 2022/7/30 23:08
 */
public class MysqlWriter extends BaseWriterPlugin implements FlinkxWriterInterface {
    @Override
    public String getName() {
        return "mysqlwriter";
    }


    @Override
    public Map<String, Object> sample() {
        return null;
    }
}

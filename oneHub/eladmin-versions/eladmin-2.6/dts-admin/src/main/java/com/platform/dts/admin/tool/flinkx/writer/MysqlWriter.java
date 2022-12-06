package com.platform.dts.admin.tool.flinkx.writer;

import java.util.Map;

/**
 * mysql writer构建类
 *
 * @author AllDataDC
 * @ClassName MysqlWriter
 * @Version 1.0
 * @since 2022/11/30 23:08
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

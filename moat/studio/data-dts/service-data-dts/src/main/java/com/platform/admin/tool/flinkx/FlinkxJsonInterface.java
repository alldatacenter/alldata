package com.platform.admin.tool.flinkx;

import java.util.Map;

/**
 * 构建 com.guoliang.flinkx json的基础接口
 *
 * @author AllDataDC
 * @ClassName FlinkxJsonHelper
 * @Version 2.1.1
 * @date 2022/03/14 12:24
 */
public interface FlinkxJsonInterface {

    Map<String, Object> buildJob();

    Map<String, Object> buildSetting();

    Map<String, Object> buildContent();

    Map<String, Object> buildReader();

    Map<String, Object> buildHiveReader();

    Map<String, Object> buildHiveWriter();

    Map<String, Object> buildHBaseReader();

    Map<String, Object> buildHBaseWriter();

    Map<String, Object> buildMongoDBReader();

    Map<String, Object> buildMongoDBWriter();

    Map<String, Object> buildWriter();
}

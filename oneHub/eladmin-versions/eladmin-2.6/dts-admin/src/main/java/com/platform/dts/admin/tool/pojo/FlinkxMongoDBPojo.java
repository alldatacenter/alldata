package com.platform.dts.admin.tool.pojo;

import com.platform.dts.admin.dto.UpsertInfo;
import com.platform.dts.admin.entity.JobDatasource;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 用于传参，构建json
 *
 * @author AllDataDC
 * @ClassName FlinkxMongoDBPojo
 * @Version 2.0
 * @date 2022/11/14 11:15
 */
@Data
public class FlinkxMongoDBPojo {

    /**
     * hive列名
     */
    private List<Map<String, Object>> columns;

    /**
     * 数据源信息
     */
    private JobDatasource jdbcDatasource;

    private String address;

    private String dbName;

    private String readerTable;

    private String writerTable;

    private UpsertInfo upsertInfo;

}
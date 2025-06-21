package com.platform.admin.tool.pojo;

import com.platform.admin.dto.UpsertInfo;
import com.platform.admin.entity.JobDatasource;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 用于传参，构建json
 *
 * @author AllDataDC
 * @ClassName FlinkxMongoDBPojo
 * @date 2022/03/14 11:15
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
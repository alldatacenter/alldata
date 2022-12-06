package com.platform.dts.admin.tool.pojo;

import com.platform.dts.admin.entity.JobDatasource;
import lombok.Data;

import java.util.List;

/**
 * 用于传参，构建json
 *
 * @author AllDataDC
 * @ClassName FlinkxRdbmsPojo
 * @Version 2.0
 * @date 2022/11/11 15:19
 */
@Data
public class FlinkxRdbmsPojo {

    /**
     * 表名
     */
    private List<String> tables;

    /**
     * 列名
     */
    private List<String> rdbmsColumns;

    /**
     * 数据源信息
     */
    private JobDatasource jobDatasource;

    /**
     * querySql 属性，如果指定了，则优先于columns参数
     */
    private String querySql;

    /**
     * preSql 属性
     */
    private String preSql;

    /**
     * postSql 属性
     */
    private String postSql;

    /**
     * 切分主键
     */
    private String splitPk;

    /**
     * where
     */
    private String whereParam;
}

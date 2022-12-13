package com.platform.dts.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.platform.dts.entity.JobDatasource;

import java.io.IOException;
import java.util.List;

/**
 * jdbc数据源配置表服务接口
 *
 * @author AllDataDC
 * @version v2.0
 * @date 2022/11/10
 */
public interface JobDatasourceService extends IService<JobDatasource> {
    /**
     * 测试数据源
     * @param jdbcDatasource
     * @return
     */
    Boolean dataSourceTest(JobDatasource jdbcDatasource) throws IOException;

    /**
     *更新数据源信息
     * @param datasource
     * @return
     */
    int update(JobDatasource datasource);

    /**
     * 获取所有数据源
     * @return
     */
    List<JobDatasource> selectAllDatasource();
}
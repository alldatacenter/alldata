package com.platform.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.admin.entity.JobDatasource;
import org.apache.ibatis.annotations.Mapper;

/**
 * jdbc数据源配置表数据库访问层
 *
 * @author AllDataDC
 * @version v1.0
 * @date 2022-07-30
 */
@Mapper
public interface JobDatasourceMapper extends BaseMapper<JobDatasource> {
    int update(JobDatasource datasource);

}
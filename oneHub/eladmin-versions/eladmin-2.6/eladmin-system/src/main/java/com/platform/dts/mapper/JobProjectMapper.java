package com.platform.dts.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.platform.dts.entity.JobProject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Project
 *
 * @author AllDataDC
 * @version v2.1.12
 * @date 2022/11/24
 */
@Mapper
public interface JobProjectMapper extends BaseMapper<JobProject> {
    /**
     * project page
     * @param page
     * @param searchName
     * @return
     */
    IPage<JobProject> getProjectListPaging(IPage<JobProject> page,
                                          @Param("searchName") String searchName);
}
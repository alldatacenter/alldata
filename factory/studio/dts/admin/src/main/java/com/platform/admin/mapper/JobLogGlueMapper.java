package com.platform.admin.mapper;

import com.platform.admin.entity.JobLogGlue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * job log for glue
 *
 * @author AllDataDC 2022-5-19 18:04:56
 */
@Mapper
public interface JobLogGlueMapper {

    int save(JobLogGlue jobLogGlue);

    List<JobLogGlue> findByJobId(@Param("jobId") int jobId);

    int removeOld(@Param("jobId") int jobId, @Param("limit") int limit);

    int deleteByJobId(@Param("jobId") int jobId);

}

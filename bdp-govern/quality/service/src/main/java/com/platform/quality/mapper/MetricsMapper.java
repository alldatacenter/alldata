package com.platform.quality.mapper;

import com.platform.quality.entity.Metrics;
import com.platform.quality.metric.model.MetricValueJson;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * MetricsMapper
 * @author wlhbdp
 * @date 2022/5/19
 **/
@Mapper
public interface MetricsMapper {

    @Insert(" INSERT INTO griffin_result \n" +
            "        (id, tmst, application_id, job_name, metadata, value) \n" +
            "        VALUES \n" +
            "        (#{metric.id}, #{metric.tmst}, #{metric.applicationId}, #{metric.jobName}, \n" +
            "#{metric.metadata, jdbcType=OTHER, typeHandler=org.apache.griffin.core.handler.MySqlJsonHandler}, \n" +
            "#{metric.value, jdbcType=OTHER, typeHandler=org.apache.griffin.core.handler.MySqlJsonHandler})")
    int insertMetrics(@Param("metric") Metrics metric);

    @Delete("DELETE FROM griffin_result WHERE job_name = #{jobName}")
    int deleteMetricsByJobName(@Param("jobName") String jobName);


    @Select("SELECT tmst as tmst, job_name as name, metadata as metaJson, value as valueJson \n" +
            "FROM `griffin_result` WHERE job_name = #{jobName}")
    List<MetricValueJson> selectMetrics(@Param("jobName") String jobName);
}

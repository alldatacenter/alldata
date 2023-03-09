package org.apache.griffin.core.mapper;

import org.apache.griffin.core.metric.model.MetricValueJson;
import org.apache.griffin.core.metric.model.MysqlMetrics;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * MetricsMapper
 * @author AllDataDC
 * @date 2022/5/19
 **/
@Mapper
public interface MetricsMapper {

    @Insert(" INSERT INTO mysql_sink \n" +
            "        (id, tmst, application_id, job_name, metadata, value) \n" +
            "        VALUES \n" +
            "        (#{metric.id}, #{metric.tmst}, #{metric.applicationId}, #{metric.jobName}, \n" +
            "#{metric.metadata, jdbcType=OTHER, typeHandler=org.apache.griffin.core.handler.MySqlJsonHandler}, \n" +
            "#{metric.value, jdbcType=OTHER, typeHandler=org.apache.griffin.core.handler.MySqlJsonHandler})")
    int insertMetrics(@Param("metric") MysqlMetrics metric);

    @Delete("DELETE FROM mysql_sink WHERE job_name = #{jobName}")
    int deleteMetricsByJobName(@Param("jobName") String jobName);


    @Select("SELECT tmst as tmst, job_name as name, metadata as metaJson, value as valueJson \n" +
            "FROM `mysql_sink` WHERE job_name = #{jobName}")
    List<MetricValueJson> selectMetrics(@Param("jobName") String jobName);
}

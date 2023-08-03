package com.netease.arctic.server.persistence.mapper;

import com.netease.arctic.optimizing.RewriteFilesInput;
import com.netease.arctic.server.optimizing.MetricsSummary;
import com.netease.arctic.server.optimizing.OptimizingProcess;
import com.netease.arctic.server.optimizing.OptimizingProcessMeta;
import com.netease.arctic.server.optimizing.OptimizingTaskMeta;
import com.netease.arctic.server.optimizing.OptimizingType;
import com.netease.arctic.server.optimizing.TaskRuntime;
import com.netease.arctic.server.persistence.converter.JsonSummaryConverter;
import com.netease.arctic.server.persistence.converter.Long2TsConverter;
import com.netease.arctic.server.persistence.converter.Map2StringConverter;
import com.netease.arctic.server.persistence.converter.MapLong2StringConverter;
import com.netease.arctic.server.persistence.converter.Object2ByteArrayConvert;
import com.netease.arctic.server.table.ServerTableIdentifier;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.type.JdbcType;

import java.util.List;
import java.util.Map;

public interface OptimizingMapper {

  /**
   * OptimizingProcess operation below
   */
  @Delete("DELETE FROM table_optimizing_process WHERE table_id = #{tableId} and process_id < #{time}")
  void deleteOptimizingProcessBefore(@Param("tableId") long tableId, @Param("time") long time);

  @Insert("INSERT INTO table_optimizing_process(table_id, catalog_name, db_name, table_name ,process_id," +
      " target_snapshot_id, target_change_snapshot_id, status, optimizing_type, plan_time, summary, from_sequence," +
      " to_sequence) VALUES (#{table.id}, #{table.catalog}," +
      " #{table.database}, #{table.tableName}, #{processId}, #{targetSnapshotId}, #{targetChangeSnapshotId}," +
      " #{status}, #{optimizingType}," +
      " #{planTime, typeHandler=com.netease.arctic.server.persistence.converter.Long2TsConverter}," +
      " #{summary, typeHandler=com.netease.arctic.server.persistence.converter.JsonSummaryConverter}," +
      " #{fromSequence, typeHandler=com.netease.arctic.server.persistence.converter.MapLong2StringConverter}," +
      " #{toSequence, typeHandler=com.netease.arctic.server.persistence.converter.MapLong2StringConverter}" +
      ")")
  void insertOptimizingProcess(
      @Param("table") ServerTableIdentifier tableIdentifier,
      @Param("processId") long processId,
      @Param("targetSnapshotId") long targetSnapshotId,
      @Param("targetChangeSnapshotId") long targetChangeSnapshotId,
      @Param("status") OptimizingProcess.Status status,
      @Param("optimizingType") OptimizingType optimizingType,
      @Param("planTime") long planTime,
      @Param("summary") MetricsSummary summary,
      @Param("fromSequence") Map<String, Long> fromSequence,
      @Param("toSequence") Map<String, Long> toSequence);

  @Update("UPDATE table_optimizing_process SET status = #{optimizingStatus}," +
      " end_time = #{endTime, typeHandler=com.netease.arctic.server.persistence.converter.Long2TsConverter}, " +
      "summary = #{summary, typeHandler=com.netease.arctic.server.persistence.converter.JsonSummaryConverter}, " +
      "fail_reason = #{failedReason, jdbcType=VARCHAR}" +
      " WHERE table_id = #{tableId} AND process_id = #{processId}")
  void updateOptimizingProcess(
      @Param("tableId") long tableId,
      @Param("processId") long processId,
      @Param("optimizingStatus") OptimizingProcess.Status status,
      @Param("endTime") long endTime,
      @Param("summary") MetricsSummary summary,
      @Param("failedReason") String failedReason);

  @Select("SELECT a.process_id, a.table_id, a.catalog_name, a.db_name, a.table_name, a.target_snapshot_id," +
      " a.target_change_snapshot_id, a.status, a.optimizing_type, a.plan_time, a.end_time," +
      " a.fail_reason, a.summary, a.from_sequence, a.to_sequence FROM table_optimizing_process a" +
      " INNER JOIN table_identifier b ON a.table_id = b.table_id" +
      " WHERE a.catalog_name = #{catalogName} AND a.db_name = #{dbName} AND a.table_name = #{tableName}" +
      " AND b.catalog_name = #{catalogName} AND b.db_name = #{dbName} AND b.table_name = #{tableName}" +
      " ORDER BY process_id desc")
  @Results({
      @Result(property = "processId", column = "process_id"),
      @Result(property = "tableId", column = "table_id"),
      @Result(property = "catalogName", column = "catalog_name"),
      @Result(property = "dbName", column = "db_name"),
      @Result(property = "tableName", column = "table_name"),
      @Result(property = "targetSnapshotId", column = "target_snapshot_id"),
      @Result(property = "targetChangeSnapshotId", column = "target_change_snapshot_id"),
      @Result(property = "status", column = "status"),
      @Result(property = "optimizingType", column = "optimizing_type"),
      @Result(property = "planTime", column = "plan_time", typeHandler = Long2TsConverter.class),
      @Result(property = "endTime", column = "end_time", typeHandler = Long2TsConverter.class),
      @Result(property = "failReason", column = "fail_reason"),
      @Result(property = "summary", column = "summary", typeHandler = JsonSummaryConverter.class),
      @Result(property = "fromSequence", column = "from_sequence",  typeHandler = MapLong2StringConverter.class),
      @Result(property = "toSequence", column = "to_sequence",  typeHandler = MapLong2StringConverter.class)
  })
  List<OptimizingProcessMeta> selectOptimizingProcesses(
      @Param("catalogName") String catalogName, @Param("dbName") String dbName, @Param("tableName") String tableName);

  /**
   * Optimizing TaskRuntime operation below
   */
  @Insert({
      "<script>",
      "INSERT INTO task_runtime (process_id, task_id, retry_num, table_id, partition_data, start_time, " +
          "end_time, status, fail_reason, optimizer_token, thread_id, rewrite_output, metrics_summary, properties) " +
          "VALUES ",
      "<foreach collection='taskRuntimes' item='taskRuntime' index='index' separator=','>",
      "(#{taskRuntime.taskId.processId}, #{taskRuntime.taskId.taskId}, #{taskRuntime.retry}," +
          " #{taskRuntime.tableId}, #{taskRuntime.partition}, " +
          "#{taskRuntime.startTime, typeHandler=com.netease.arctic.server.persistence.converter.Long2TsConverter}," +
          " #{taskRuntime.endTime, typeHandler=com.netease.arctic.server.persistence.converter.Long2TsConverter}, " +
          "#{taskRuntime.status}, #{taskRuntime.failReason, jdbcType=VARCHAR}," +
          " #{taskRuntime.optimizingThread.token, jdbcType=VARCHAR}, #{taskRuntime.optimizingThread.threadId, " +
          "jdbcType=INTEGER}, #{taskRuntime.output, jdbcType=BLOB, " +
          " typeHandler=com.netease.arctic.server.persistence.converter.Object2ByteArrayConvert}," +
          " #{taskRuntime.summary, typeHandler=com.netease.arctic.server.persistence.converter.JsonSummaryConverter}," +
          "#{taskRuntime.properties, typeHandler=com.netease.arctic.server.persistence.converter.Map2StringConverter})",
      "</foreach>",
      "</script>"
  })
  void insertTaskRuntimes(@Param("taskRuntimes") List<TaskRuntime> taskRuntimes);

  @Select("SELECT process_id, task_id, retry_num, table_id, partition_data,  create_time, start_time, end_time," +
      " status, fail_reason, optimizer_token, thread_id, rewrite_output, metrics_summary, properties FROM " +
      "task_runtime WHERE table_id = #{table_id} AND process_id = #{process_id}")
  @Results({
      @Result(property = "taskId.processId", column = "process_id"),
      @Result(property = "taskId.taskId", column = "task_id"),
      @Result(property = "retry", column = "retry_num"),
      @Result(property = "tableId", column = "table_id"),
      @Result(property = "partition", column = "partition_data"),
      @Result(property = "startTime", column = "start_time", typeHandler = Long2TsConverter.class),
      @Result(property = "endTime", column = "end_time", typeHandler = Long2TsConverter.class),
      @Result(property = "status", column = "status"),
      @Result(property = "failReason", column = "fail_reason"),
      @Result(property = "optimizingThread.token", column = "optimizer_token"),
      @Result(property = "optimizingThread.threadId", column = "thread_id"),
      @Result(property = "output", column = "rewrite_output", typeHandler = Object2ByteArrayConvert.class),
      @Result(property = "summary", column = "metrics_summary", typeHandler = JsonSummaryConverter.class),
      @Result(property = "properties", column = "properties", typeHandler = Map2StringConverter.class)
  })
  List<TaskRuntime> selectTaskRuntimes(@Param("table_id") long tableId, @Param("process_id") long processId);

  @Select("<script>" +
      "SELECT process_id, task_id, retry_num, table_id, partition_data, create_time, start_time, end_time, " +
      "cost_time, status, fail_reason, optimizer_token, thread_id, metrics_summary, properties FROM task_runtime " +
      "WHERE process_id IN" +
      "<foreach item='item' index='index' collection='processIds' open='(' separator=',' close=')'>" +
      "#{item}" +
      "</foreach>" +
      "</script>")
  @Results({
      @Result(property = "processId", column = "process_id"),
      @Result(property = "taskId", column = "task_id"),
      @Result(property = "retryNum", column = "retry_num"),
      @Result(property = "tableId", column = "table_id"),
      @Result(property = "partitionData", column = "partition_data"),
      @Result(property = "createTime", column = "create_time", typeHandler = Long2TsConverter.class),
      @Result(property = "startTime", column = "start_time", typeHandler = Long2TsConverter.class),
      @Result(property = "endTime", column = "end_time", typeHandler = Long2TsConverter.class),
      @Result(property = "costTime", column = "cost_time"),
      @Result(property = "status", column = "status"),
      @Result(property = "failReason", column = "fail_reason"),
      @Result(property = "optimizerToken", column = "optimizer_token"),
      @Result(property = "threadId", column = "thread_id"),
      @Result(property = "metricsSummary", column = "metrics_summary", typeHandler = JsonSummaryConverter.class),
      @Result(property = "properties", column = "properties", typeHandler = Map2StringConverter.class)
  })
  List<OptimizingTaskMeta> selectOptimizeTaskMetas(@Param("processIds") List<Long> processIds);

  @Update("UPDATE task_runtime SET retry_num = #{taskRuntime.retry}, " +
      "start_time = #{taskRuntime.startTime," +
      " typeHandler=com.netease.arctic.server.persistence.converter.Long2TsConverter}," +
      " end_time = #{taskRuntime.endTime," +
      " typeHandler=com.netease.arctic.server.persistence.converter.Long2TsConverter}," +
      " cost_time = #{taskRuntime.costTime}, status = #{taskRuntime.status}," +
      " fail_reason = #{taskRuntime.failReason, jdbcType=VARCHAR}," +
      " optimizer_token = #{taskRuntime.optimizingThread.token, jdbcType=VARCHAR}," +
      " thread_id = #{taskRuntime.optimizingThread.threadId, jdbcType=INTEGER}," +
      " rewrite_output = #{taskRuntime.output, jdbcType=BLOB," +
      " typeHandler=com.netease.arctic.server.persistence.converter.Object2ByteArrayConvert}," +
      " metrics_summary = #{taskRuntime.summary," +
      " typeHandler=com.netease.arctic.server.persistence.converter.JsonSummaryConverter}," +
      " properties = #{taskRuntime.properties," +
      " typeHandler=com.netease.arctic.server.persistence.converter.Map2StringConverter}" +
      " WHERE process_id = #{taskRuntime.taskId.processId} AND " +
      "task_id = #{taskRuntime.taskId.taskId}")
  void updateTaskRuntime(@Param("taskRuntime") TaskRuntime taskRuntime);

  @Update("UPDATE task_runtime SET status = #{status} WHERE process_id = #{taskRuntime.taskId.processId} AND " +
      "task_id = #{taskRuntime.taskId.taskId}")
  void updateTaskStatus(@Param("taskRuntime") TaskRuntime taskRuntime, @Param("status") TaskRuntime.Status status);

  @Delete("DELETE FROM task_runtime WHERE table_id = #{tableId} AND process_id < #{time}")
  void deleteTaskRuntimesBefore(@Param("tableId") long tableId, @Param("time") long time);

  /**
   * Optimizing rewrite input and output operations below
   */
  @Update("UPDATE table_optimizing_process SET rewrite_input = #{input, jdbcType=BLOB," +
      " typeHandler=com.netease.arctic.server.persistence.converter.Object2ByteArrayConvert}" +
      " WHERE process_id = #{processId}")
  void updateProcessInputFiles(
      @Param("processId") long processId,
      @Param("input") Map<Integer, RewriteFilesInput> input);

  @Select("SELECT rewrite_input FROM table_optimizing_process WHERE process_id = #{processId}")
  @Results({
      @Result(column = "rewrite_input", jdbcType = JdbcType.BLOB)
  })
  List<byte[]> selectProcessInputFiles(@Param("processId") long processId);

  /**
   * Optimizing task quota operations below
   */
  @Select("SELECT process_id, task_id, retry_num, table_id, start_time, end_time, fail_reason " +
      "FROM optimizing_task_quota WHERE table_id = #{tableId} AND process_id >= #{startTime}")
  @Results({
      @Result(property = "processId", column = "process_id"),
      @Result(property = "taskId", column = "task_id"),
      @Result(property = "retryNum", column = "retry_num"),
      @Result(property = "tableId", column = "table_id"),
      @Result(property = "startTime", column = "start_time", typeHandler = Long2TsConverter.class),
      @Result(property = "endTime", column = "end_time", typeHandler = Long2TsConverter.class),
      @Result(property = "failReason", column = "fail_reason")
  })
  List<TaskRuntime.TaskQuota> selectTaskQuotasByTime(
      @Param("tableId") long tableId,
      @Param("startTime") long startTime);

  @Insert("INSERT INTO optimizing_task_quota (process_id, task_id, retry_num, table_id, start_time, end_time," +
      " fail_reason) VALUES (#{taskQuota.processId}, #{taskQuota.taskId}, #{taskQuota.retryNum}," +
      " #{taskQuota.tableId}," +
      " #{taskQuota.startTime, typeHandler=com.netease.arctic.server.persistence.converter.Long2TsConverter}, " +
      " #{taskQuota.endTime, typeHandler=com.netease.arctic.server.persistence.converter.Long2TsConverter}," +
      " #{taskQuota.failReason, jdbcType=VARCHAR})")
  void insertTaskQuota(@Param("taskQuota") TaskRuntime.TaskQuota taskQuota);

  @Delete("DELETE FROM optimizing_task_quota WHERE table_id = #{table_id} AND process_id < #{time}")
  void deleteOptimizingQuotaBefore(@Param("table_id") long tableId, @Param("time") long timestamp);
}

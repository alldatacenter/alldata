package com.hw.lineage.server.infrastructure.persistence.mapper;

import com.hw.lineage.server.infrastructure.persistence.dos.TaskDO;
import com.hw.lineage.server.infrastructure.persistence.mybatis.handler.impl.ColumnGraphTypeHandler;
import com.hw.lineage.server.infrastructure.persistence.mybatis.handler.impl.TableGraphTypeHandler;
import com.hw.lineage.server.infrastructure.persistence.mybatis.handler.impl.TaskStatusTypeHandler;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.dynamic.sql.BasicColumn;
import org.mybatis.dynamic.sql.delete.DeleteDSLCompleter;
import org.mybatis.dynamic.sql.insert.render.InsertStatementProvider;
import org.mybatis.dynamic.sql.select.CountDSLCompleter;
import org.mybatis.dynamic.sql.select.SelectDSLCompleter;
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider;
import org.mybatis.dynamic.sql.update.UpdateDSL;
import org.mybatis.dynamic.sql.update.UpdateDSLCompleter;
import org.mybatis.dynamic.sql.update.UpdateModel;
import org.mybatis.dynamic.sql.util.SqlProviderAdapter;
import org.mybatis.dynamic.sql.util.mybatis3.CommonCountMapper;
import org.mybatis.dynamic.sql.util.mybatis3.CommonDeleteMapper;
import org.mybatis.dynamic.sql.util.mybatis3.CommonUpdateMapper;
import org.mybatis.dynamic.sql.util.mybatis3.MyBatis3Utils;

import java.util.List;
import java.util.Optional;

import static com.hw.lineage.server.infrastructure.persistence.mapper.TaskDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;

@Mapper
public interface TaskMapper extends CommonCountMapper, CommonDeleteMapper, CommonUpdateMapper {
    BasicColumn[] selectList = BasicColumn.columnList(taskId, catalogId, taskName, descr, database, taskStatus, lineageTime, createUserId, modifyUserId, createTime, modifyTime, invalid, taskSource, taskLog, tableGraph, columnGraph);

    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="row.taskId", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<TaskDO> insertStatement);

    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @Results(id="TaskDOResult", value = {
        @Result(column="task_id", property="taskId", jdbcType=JdbcType.BIGINT, id=true),
        @Result(column="catalog_id", property="catalogId", jdbcType=JdbcType.BIGINT),
        @Result(column="task_name", property="taskName", jdbcType=JdbcType.VARCHAR),
        @Result(column="descr", property="descr", jdbcType=JdbcType.VARCHAR),
        @Result(column="database", property="database", jdbcType=JdbcType.VARCHAR),
        @Result(column="task_status", property="taskStatus", typeHandler=TaskStatusTypeHandler.class, jdbcType=JdbcType.TINYINT),
        @Result(column="lineage_time", property="lineageTime", jdbcType=JdbcType.BIGINT),
        @Result(column="create_user_id", property="createUserId", jdbcType=JdbcType.BIGINT),
        @Result(column="modify_user_id", property="modifyUserId", jdbcType=JdbcType.BIGINT),
        @Result(column="create_time", property="createTime", jdbcType=JdbcType.BIGINT),
        @Result(column="modify_time", property="modifyTime", jdbcType=JdbcType.BIGINT),
        @Result(column="invalid", property="invalid", jdbcType=JdbcType.BIT),
        @Result(column="task_source", property="taskSource", jdbcType=JdbcType.LONGVARCHAR),
        @Result(column="task_log", property="taskLog", jdbcType=JdbcType.LONGVARCHAR),
        @Result(column="table_graph", property="tableGraph", typeHandler=TableGraphTypeHandler.class, jdbcType=JdbcType.LONGVARCHAR),
        @Result(column="column_graph", property="columnGraph", typeHandler=ColumnGraphTypeHandler.class, jdbcType=JdbcType.LONGVARCHAR)
    })
    List<TaskDO> selectMany(SelectStatementProvider selectStatement);

    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ResultMap("TaskDOResult")
    Optional<TaskDO> selectOne(SelectStatementProvider selectStatement);

    default long count(CountDSLCompleter completer) {
        return MyBatis3Utils.countFrom(this::count, task, completer);
    }

    default int delete(DeleteDSLCompleter completer) {
        return MyBatis3Utils.deleteFrom(this::delete, task, completer);
    }

    default int deleteByPrimaryKey(Long taskId_) {
        return delete(c -> 
            c.where(taskId, isEqualTo(taskId_))
        );
    }

    default int insert(TaskDO row) {
        return MyBatis3Utils.insert(this::insert, row, task, c ->
            c.map(catalogId).toProperty("catalogId")
            .map(taskName).toProperty("taskName")
            .map(descr).toProperty("descr")
            .map(database).toProperty("database")
            .map(taskStatus).toProperty("taskStatus")
            .map(lineageTime).toProperty("lineageTime")
            .map(createUserId).toProperty("createUserId")
            .map(modifyUserId).toProperty("modifyUserId")
            .map(createTime).toProperty("createTime")
            .map(modifyTime).toProperty("modifyTime")
            .map(invalid).toProperty("invalid")
            .map(taskSource).toProperty("taskSource")
            .map(taskLog).toProperty("taskLog")
            .map(tableGraph).toProperty("tableGraph")
            .map(columnGraph).toProperty("columnGraph")
        );
    }

    default int insertSelective(TaskDO row) {
        return MyBatis3Utils.insert(this::insert, row, task, c ->
            c.map(catalogId).toPropertyWhenPresent("catalogId", row::getCatalogId)
            .map(taskName).toPropertyWhenPresent("taskName", row::getTaskName)
            .map(descr).toPropertyWhenPresent("descr", row::getDescr)
            .map(database).toPropertyWhenPresent("database", row::getDatabase)
            .map(taskStatus).toPropertyWhenPresent("taskStatus", row::getTaskStatus)
            .map(lineageTime).toPropertyWhenPresent("lineageTime", row::getLineageTime)
            .map(createUserId).toPropertyWhenPresent("createUserId", row::getCreateUserId)
            .map(modifyUserId).toPropertyWhenPresent("modifyUserId", row::getModifyUserId)
            .map(createTime).toPropertyWhenPresent("createTime", row::getCreateTime)
            .map(modifyTime).toPropertyWhenPresent("modifyTime", row::getModifyTime)
            .map(invalid).toPropertyWhenPresent("invalid", row::getInvalid)
            .map(taskSource).toPropertyWhenPresent("taskSource", row::getTaskSource)
            .map(taskLog).toPropertyWhenPresent("taskLog", row::getTaskLog)
            .map(tableGraph).toPropertyWhenPresent("tableGraph", row::getTableGraph)
            .map(columnGraph).toPropertyWhenPresent("columnGraph", row::getColumnGraph)
        );
    }

    default Optional<TaskDO> selectOne(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectOne(this::selectOne, selectList, task, completer);
    }

    default List<TaskDO> select(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectList(this::selectMany, selectList, task, completer);
    }

    default List<TaskDO> selectDistinct(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectDistinct(this::selectMany, selectList, task, completer);
    }

    default Optional<TaskDO> selectByPrimaryKey(Long taskId_) {
        return selectOne(c ->
            c.where(taskId, isEqualTo(taskId_))
        );
    }

    default int update(UpdateDSLCompleter completer) {
        return MyBatis3Utils.update(this::update, task, completer);
    }

    static UpdateDSL<UpdateModel> updateAllColumns(TaskDO row, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(catalogId).equalTo(row::getCatalogId)
                .set(taskName).equalTo(row::getTaskName)
                .set(descr).equalTo(row::getDescr)
                .set(database).equalTo(row::getDatabase)
                .set(taskStatus).equalTo(row::getTaskStatus)
                .set(lineageTime).equalTo(row::getLineageTime)
                .set(createUserId).equalTo(row::getCreateUserId)
                .set(modifyUserId).equalTo(row::getModifyUserId)
                .set(createTime).equalTo(row::getCreateTime)
                .set(modifyTime).equalTo(row::getModifyTime)
                .set(invalid).equalTo(row::getInvalid)
                .set(taskSource).equalTo(row::getTaskSource)
                .set(taskLog).equalTo(row::getTaskLog)
                .set(tableGraph).equalTo(row::getTableGraph)
                .set(columnGraph).equalTo(row::getColumnGraph);
    }

    static UpdateDSL<UpdateModel> updateSelectiveColumns(TaskDO row, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(catalogId).equalToWhenPresent(row::getCatalogId)
                .set(taskName).equalToWhenPresent(row::getTaskName)
                .set(descr).equalToWhenPresent(row::getDescr)
                .set(database).equalToWhenPresent(row::getDatabase)
                .set(taskStatus).equalToWhenPresent(row::getTaskStatus)
                .set(lineageTime).equalToWhenPresent(row::getLineageTime)
                .set(createUserId).equalToWhenPresent(row::getCreateUserId)
                .set(modifyUserId).equalToWhenPresent(row::getModifyUserId)
                .set(createTime).equalToWhenPresent(row::getCreateTime)
                .set(modifyTime).equalToWhenPresent(row::getModifyTime)
                .set(invalid).equalToWhenPresent(row::getInvalid)
                .set(taskSource).equalToWhenPresent(row::getTaskSource)
                .set(taskLog).equalToWhenPresent(row::getTaskLog)
                .set(tableGraph).equalToWhenPresent(row::getTableGraph)
                .set(columnGraph).equalToWhenPresent(row::getColumnGraph);
    }

    default int updateByPrimaryKey(TaskDO row) {
        return update(c ->
            c.set(catalogId).equalTo(row::getCatalogId)
            .set(taskName).equalTo(row::getTaskName)
            .set(descr).equalTo(row::getDescr)
            .set(database).equalTo(row::getDatabase)
            .set(taskStatus).equalTo(row::getTaskStatus)
            .set(lineageTime).equalTo(row::getLineageTime)
            .set(createUserId).equalTo(row::getCreateUserId)
            .set(modifyUserId).equalTo(row::getModifyUserId)
            .set(createTime).equalTo(row::getCreateTime)
            .set(modifyTime).equalTo(row::getModifyTime)
            .set(invalid).equalTo(row::getInvalid)
            .set(taskSource).equalTo(row::getTaskSource)
            .set(taskLog).equalTo(row::getTaskLog)
            .set(tableGraph).equalTo(row::getTableGraph)
            .set(columnGraph).equalTo(row::getColumnGraph)
            .where(taskId, isEqualTo(row::getTaskId))
        );
    }

    default int updateByPrimaryKeySelective(TaskDO row) {
        return update(c ->
            c.set(catalogId).equalToWhenPresent(row::getCatalogId)
            .set(taskName).equalToWhenPresent(row::getTaskName)
            .set(descr).equalToWhenPresent(row::getDescr)
            .set(database).equalToWhenPresent(row::getDatabase)
            .set(taskStatus).equalToWhenPresent(row::getTaskStatus)
            .set(lineageTime).equalToWhenPresent(row::getLineageTime)
            .set(createUserId).equalToWhenPresent(row::getCreateUserId)
            .set(modifyUserId).equalToWhenPresent(row::getModifyUserId)
            .set(createTime).equalToWhenPresent(row::getCreateTime)
            .set(modifyTime).equalToWhenPresent(row::getModifyTime)
            .set(invalid).equalToWhenPresent(row::getInvalid)
            .set(taskSource).equalToWhenPresent(row::getTaskSource)
            .set(taskLog).equalToWhenPresent(row::getTaskLog)
            .set(tableGraph).equalToWhenPresent(row::getTableGraph)
            .set(columnGraph).equalToWhenPresent(row::getColumnGraph)
            .where(taskId, isEqualTo(row::getTaskId))
        );
    }
}
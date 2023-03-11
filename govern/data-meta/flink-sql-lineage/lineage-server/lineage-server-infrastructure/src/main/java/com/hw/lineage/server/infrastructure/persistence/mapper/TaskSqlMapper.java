package com.hw.lineage.server.infrastructure.persistence.mapper;

import static com.hw.lineage.server.infrastructure.persistence.mapper.TaskSqlDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;

import com.hw.lineage.server.infrastructure.persistence.dos.TaskSqlDO;
import com.hw.lineage.server.infrastructure.persistence.mybatis.handler.impl.SqlStatusTypeHandler;
import com.hw.lineage.server.infrastructure.persistence.mybatis.handler.impl.SqlTypeHandler;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.SelectKey;
import org.apache.ibatis.annotations.SelectProvider;
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

@Mapper
public interface TaskSqlMapper extends CommonCountMapper, CommonDeleteMapper, CommonUpdateMapper {
    BasicColumn[] selectList = BasicColumn.columnList(sqlId, taskId, sqlType, startLineNumber, sqlStatus, invalid, sqlSource);

    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="row.sqlId", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<TaskSqlDO> insertStatement);

    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @Results(id="TaskSqlDOResult", value = {
        @Result(column="sql_id", property="sqlId", jdbcType=JdbcType.BIGINT, id=true),
        @Result(column="task_id", property="taskId", jdbcType=JdbcType.BIGINT),
        @Result(column="sql_type", property="sqlType", typeHandler=SqlTypeHandler.class, jdbcType=JdbcType.VARCHAR),
        @Result(column="start_line_number", property="startLineNumber", jdbcType=JdbcType.BIGINT),
        @Result(column="sql_status", property="sqlStatus", typeHandler=SqlStatusTypeHandler.class, jdbcType=JdbcType.TINYINT),
        @Result(column="invalid", property="invalid", jdbcType=JdbcType.BIT),
        @Result(column="sql_source", property="sqlSource", jdbcType=JdbcType.LONGVARCHAR)
    })
    List<TaskSqlDO> selectMany(SelectStatementProvider selectStatement);

    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ResultMap("TaskSqlDOResult")
    Optional<TaskSqlDO> selectOne(SelectStatementProvider selectStatement);

    default long count(CountDSLCompleter completer) {
        return MyBatis3Utils.countFrom(this::count, taskSql, completer);
    }

    default int delete(DeleteDSLCompleter completer) {
        return MyBatis3Utils.deleteFrom(this::delete, taskSql, completer);
    }

    default int deleteByPrimaryKey(Long sqlId_) {
        return delete(c -> 
            c.where(sqlId, isEqualTo(sqlId_))
        );
    }

    default int insert(TaskSqlDO row) {
        return MyBatis3Utils.insert(this::insert, row, taskSql, c ->
            c.map(taskId).toProperty("taskId")
            .map(sqlType).toProperty("sqlType")
            .map(startLineNumber).toProperty("startLineNumber")
            .map(sqlStatus).toProperty("sqlStatus")
            .map(invalid).toProperty("invalid")
            .map(sqlSource).toProperty("sqlSource")
        );
    }

    default int insertSelective(TaskSqlDO row) {
        return MyBatis3Utils.insert(this::insert, row, taskSql, c ->
            c.map(taskId).toPropertyWhenPresent("taskId", row::getTaskId)
            .map(sqlType).toPropertyWhenPresent("sqlType", row::getSqlType)
            .map(startLineNumber).toPropertyWhenPresent("startLineNumber", row::getStartLineNumber)
            .map(sqlStatus).toPropertyWhenPresent("sqlStatus", row::getSqlStatus)
            .map(invalid).toPropertyWhenPresent("invalid", row::getInvalid)
            .map(sqlSource).toPropertyWhenPresent("sqlSource", row::getSqlSource)
        );
    }

    default Optional<TaskSqlDO> selectOne(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectOne(this::selectOne, selectList, taskSql, completer);
    }

    default List<TaskSqlDO> select(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectList(this::selectMany, selectList, taskSql, completer);
    }

    default List<TaskSqlDO> selectDistinct(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectDistinct(this::selectMany, selectList, taskSql, completer);
    }

    default Optional<TaskSqlDO> selectByPrimaryKey(Long sqlId_) {
        return selectOne(c ->
            c.where(sqlId, isEqualTo(sqlId_))
        );
    }

    default int update(UpdateDSLCompleter completer) {
        return MyBatis3Utils.update(this::update, taskSql, completer);
    }

    static UpdateDSL<UpdateModel> updateAllColumns(TaskSqlDO row, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(taskId).equalTo(row::getTaskId)
                .set(sqlType).equalTo(row::getSqlType)
                .set(startLineNumber).equalTo(row::getStartLineNumber)
                .set(sqlStatus).equalTo(row::getSqlStatus)
                .set(invalid).equalTo(row::getInvalid)
                .set(sqlSource).equalTo(row::getSqlSource);
    }

    static UpdateDSL<UpdateModel> updateSelectiveColumns(TaskSqlDO row, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(taskId).equalToWhenPresent(row::getTaskId)
                .set(sqlType).equalToWhenPresent(row::getSqlType)
                .set(startLineNumber).equalToWhenPresent(row::getStartLineNumber)
                .set(sqlStatus).equalToWhenPresent(row::getSqlStatus)
                .set(invalid).equalToWhenPresent(row::getInvalid)
                .set(sqlSource).equalToWhenPresent(row::getSqlSource);
    }

    default int updateByPrimaryKey(TaskSqlDO row) {
        return update(c ->
            c.set(taskId).equalTo(row::getTaskId)
            .set(sqlType).equalTo(row::getSqlType)
            .set(startLineNumber).equalTo(row::getStartLineNumber)
            .set(sqlStatus).equalTo(row::getSqlStatus)
            .set(invalid).equalTo(row::getInvalid)
            .set(sqlSource).equalTo(row::getSqlSource)
            .where(sqlId, isEqualTo(row::getSqlId))
        );
    }

    default int updateByPrimaryKeySelective(TaskSqlDO row) {
        return update(c ->
            c.set(taskId).equalToWhenPresent(row::getTaskId)
            .set(sqlType).equalToWhenPresent(row::getSqlType)
            .set(startLineNumber).equalToWhenPresent(row::getStartLineNumber)
            .set(sqlStatus).equalToWhenPresent(row::getSqlStatus)
            .set(invalid).equalToWhenPresent(row::getInvalid)
            .set(sqlSource).equalToWhenPresent(row::getSqlSource)
            .where(sqlId, isEqualTo(row::getSqlId))
        );
    }
}
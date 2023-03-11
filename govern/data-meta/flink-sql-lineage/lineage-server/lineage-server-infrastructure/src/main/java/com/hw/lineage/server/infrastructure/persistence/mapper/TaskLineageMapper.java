package com.hw.lineage.server.infrastructure.persistence.mapper;

import static com.hw.lineage.server.infrastructure.persistence.mapper.TaskLineageDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;

import com.hw.lineage.server.infrastructure.persistence.dos.TaskLineageDO;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.dynamic.sql.BasicColumn;
import org.mybatis.dynamic.sql.delete.DeleteDSLCompleter;
import org.mybatis.dynamic.sql.select.CountDSLCompleter;
import org.mybatis.dynamic.sql.select.SelectDSLCompleter;
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider;
import org.mybatis.dynamic.sql.update.UpdateDSL;
import org.mybatis.dynamic.sql.update.UpdateDSLCompleter;
import org.mybatis.dynamic.sql.update.UpdateModel;
import org.mybatis.dynamic.sql.util.SqlProviderAdapter;
import org.mybatis.dynamic.sql.util.mybatis3.CommonCountMapper;
import org.mybatis.dynamic.sql.util.mybatis3.CommonDeleteMapper;
import org.mybatis.dynamic.sql.util.mybatis3.CommonInsertMapper;
import org.mybatis.dynamic.sql.util.mybatis3.CommonUpdateMapper;
import org.mybatis.dynamic.sql.util.mybatis3.MyBatis3Utils;

@Mapper
public interface TaskLineageMapper extends CommonCountMapper, CommonDeleteMapper, CommonInsertMapper<TaskLineageDO>, CommonUpdateMapper {
    BasicColumn[] selectList = BasicColumn.columnList(rid, taskId, sqlId, sourceCatalog, sourceDatabase, sourceTable, sourceColumn, targetCatalog, targetDatabase, targetTable, targetColumn, transform, invalid);

    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @Results(id="TaskLineageDOResult", value = {
        @Result(column="rid", property="rid", jdbcType=JdbcType.BIGINT, id=true),
        @Result(column="task_id", property="taskId", jdbcType=JdbcType.BIGINT),
        @Result(column="sql_id", property="sqlId", jdbcType=JdbcType.BIGINT),
        @Result(column="source_catalog", property="sourceCatalog", jdbcType=JdbcType.VARCHAR),
        @Result(column="source_database", property="sourceDatabase", jdbcType=JdbcType.VARCHAR),
        @Result(column="source_table", property="sourceTable", jdbcType=JdbcType.VARCHAR),
        @Result(column="source_column", property="sourceColumn", jdbcType=JdbcType.VARCHAR),
        @Result(column="target_catalog", property="targetCatalog", jdbcType=JdbcType.VARCHAR),
        @Result(column="target_database", property="targetDatabase", jdbcType=JdbcType.VARCHAR),
        @Result(column="target_table", property="targetTable", jdbcType=JdbcType.VARCHAR),
        @Result(column="target_column", property="targetColumn", jdbcType=JdbcType.VARCHAR),
        @Result(column="transform", property="transform", jdbcType=JdbcType.VARCHAR),
        @Result(column="invalid", property="invalid", jdbcType=JdbcType.BIT)
    })
    List<TaskLineageDO> selectMany(SelectStatementProvider selectStatement);

    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ResultMap("TaskLineageDOResult")
    Optional<TaskLineageDO> selectOne(SelectStatementProvider selectStatement);

    default long count(CountDSLCompleter completer) {
        return MyBatis3Utils.countFrom(this::count, taskLineage, completer);
    }

    default int delete(DeleteDSLCompleter completer) {
        return MyBatis3Utils.deleteFrom(this::delete, taskLineage, completer);
    }

    default int deleteByPrimaryKey(Long rid_) {
        return delete(c -> 
            c.where(rid, isEqualTo(rid_))
        );
    }

    default int insert(TaskLineageDO row) {
        return MyBatis3Utils.insert(this::insert, row, taskLineage, c ->
            c.map(rid).toProperty("rid")
            .map(taskId).toProperty("taskId")
            .map(sqlId).toProperty("sqlId")
            .map(sourceCatalog).toProperty("sourceCatalog")
            .map(sourceDatabase).toProperty("sourceDatabase")
            .map(sourceTable).toProperty("sourceTable")
            .map(sourceColumn).toProperty("sourceColumn")
            .map(targetCatalog).toProperty("targetCatalog")
            .map(targetDatabase).toProperty("targetDatabase")
            .map(targetTable).toProperty("targetTable")
            .map(targetColumn).toProperty("targetColumn")
            .map(transform).toProperty("transform")
            .map(invalid).toProperty("invalid")
        );
    }

    default int insertMultiple(Collection<TaskLineageDO> records) {
        return MyBatis3Utils.insertMultiple(this::insertMultiple, records, taskLineage, c ->
            c.map(rid).toProperty("rid")
            .map(taskId).toProperty("taskId")
            .map(sqlId).toProperty("sqlId")
            .map(sourceCatalog).toProperty("sourceCatalog")
            .map(sourceDatabase).toProperty("sourceDatabase")
            .map(sourceTable).toProperty("sourceTable")
            .map(sourceColumn).toProperty("sourceColumn")
            .map(targetCatalog).toProperty("targetCatalog")
            .map(targetDatabase).toProperty("targetDatabase")
            .map(targetTable).toProperty("targetTable")
            .map(targetColumn).toProperty("targetColumn")
            .map(transform).toProperty("transform")
            .map(invalid).toProperty("invalid")
        );
    }

    default int insertSelective(TaskLineageDO row) {
        return MyBatis3Utils.insert(this::insert, row, taskLineage, c ->
            c.map(rid).toPropertyWhenPresent("rid", row::getRid)
            .map(taskId).toPropertyWhenPresent("taskId", row::getTaskId)
            .map(sqlId).toPropertyWhenPresent("sqlId", row::getSqlId)
            .map(sourceCatalog).toPropertyWhenPresent("sourceCatalog", row::getSourceCatalog)
            .map(sourceDatabase).toPropertyWhenPresent("sourceDatabase", row::getSourceDatabase)
            .map(sourceTable).toPropertyWhenPresent("sourceTable", row::getSourceTable)
            .map(sourceColumn).toPropertyWhenPresent("sourceColumn", row::getSourceColumn)
            .map(targetCatalog).toPropertyWhenPresent("targetCatalog", row::getTargetCatalog)
            .map(targetDatabase).toPropertyWhenPresent("targetDatabase", row::getTargetDatabase)
            .map(targetTable).toPropertyWhenPresent("targetTable", row::getTargetTable)
            .map(targetColumn).toPropertyWhenPresent("targetColumn", row::getTargetColumn)
            .map(transform).toPropertyWhenPresent("transform", row::getTransform)
            .map(invalid).toPropertyWhenPresent("invalid", row::getInvalid)
        );
    }

    default Optional<TaskLineageDO> selectOne(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectOne(this::selectOne, selectList, taskLineage, completer);
    }

    default List<TaskLineageDO> select(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectList(this::selectMany, selectList, taskLineage, completer);
    }

    default List<TaskLineageDO> selectDistinct(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectDistinct(this::selectMany, selectList, taskLineage, completer);
    }

    default Optional<TaskLineageDO> selectByPrimaryKey(Long rid_) {
        return selectOne(c ->
            c.where(rid, isEqualTo(rid_))
        );
    }

    default int update(UpdateDSLCompleter completer) {
        return MyBatis3Utils.update(this::update, taskLineage, completer);
    }

    static UpdateDSL<UpdateModel> updateAllColumns(TaskLineageDO row, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(rid).equalTo(row::getRid)
                .set(taskId).equalTo(row::getTaskId)
                .set(sqlId).equalTo(row::getSqlId)
                .set(sourceCatalog).equalTo(row::getSourceCatalog)
                .set(sourceDatabase).equalTo(row::getSourceDatabase)
                .set(sourceTable).equalTo(row::getSourceTable)
                .set(sourceColumn).equalTo(row::getSourceColumn)
                .set(targetCatalog).equalTo(row::getTargetCatalog)
                .set(targetDatabase).equalTo(row::getTargetDatabase)
                .set(targetTable).equalTo(row::getTargetTable)
                .set(targetColumn).equalTo(row::getTargetColumn)
                .set(transform).equalTo(row::getTransform)
                .set(invalid).equalTo(row::getInvalid);
    }

    static UpdateDSL<UpdateModel> updateSelectiveColumns(TaskLineageDO row, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(rid).equalToWhenPresent(row::getRid)
                .set(taskId).equalToWhenPresent(row::getTaskId)
                .set(sqlId).equalToWhenPresent(row::getSqlId)
                .set(sourceCatalog).equalToWhenPresent(row::getSourceCatalog)
                .set(sourceDatabase).equalToWhenPresent(row::getSourceDatabase)
                .set(sourceTable).equalToWhenPresent(row::getSourceTable)
                .set(sourceColumn).equalToWhenPresent(row::getSourceColumn)
                .set(targetCatalog).equalToWhenPresent(row::getTargetCatalog)
                .set(targetDatabase).equalToWhenPresent(row::getTargetDatabase)
                .set(targetTable).equalToWhenPresent(row::getTargetTable)
                .set(targetColumn).equalToWhenPresent(row::getTargetColumn)
                .set(transform).equalToWhenPresent(row::getTransform)
                .set(invalid).equalToWhenPresent(row::getInvalid);
    }

    default int updateByPrimaryKey(TaskLineageDO row) {
        return update(c ->
            c.set(taskId).equalTo(row::getTaskId)
            .set(sqlId).equalTo(row::getSqlId)
            .set(sourceCatalog).equalTo(row::getSourceCatalog)
            .set(sourceDatabase).equalTo(row::getSourceDatabase)
            .set(sourceTable).equalTo(row::getSourceTable)
            .set(sourceColumn).equalTo(row::getSourceColumn)
            .set(targetCatalog).equalTo(row::getTargetCatalog)
            .set(targetDatabase).equalTo(row::getTargetDatabase)
            .set(targetTable).equalTo(row::getTargetTable)
            .set(targetColumn).equalTo(row::getTargetColumn)
            .set(transform).equalTo(row::getTransform)
            .set(invalid).equalTo(row::getInvalid)
            .where(rid, isEqualTo(row::getRid))
        );
    }

    default int updateByPrimaryKeySelective(TaskLineageDO row) {
        return update(c ->
            c.set(taskId).equalToWhenPresent(row::getTaskId)
            .set(sqlId).equalToWhenPresent(row::getSqlId)
            .set(sourceCatalog).equalToWhenPresent(row::getSourceCatalog)
            .set(sourceDatabase).equalToWhenPresent(row::getSourceDatabase)
            .set(sourceTable).equalToWhenPresent(row::getSourceTable)
            .set(sourceColumn).equalToWhenPresent(row::getSourceColumn)
            .set(targetCatalog).equalToWhenPresent(row::getTargetCatalog)
            .set(targetDatabase).equalToWhenPresent(row::getTargetDatabase)
            .set(targetTable).equalToWhenPresent(row::getTargetTable)
            .set(targetColumn).equalToWhenPresent(row::getTargetColumn)
            .set(transform).equalToWhenPresent(row::getTransform)
            .set(invalid).equalToWhenPresent(row::getInvalid)
            .where(rid, isEqualTo(row::getRid))
        );
    }
}
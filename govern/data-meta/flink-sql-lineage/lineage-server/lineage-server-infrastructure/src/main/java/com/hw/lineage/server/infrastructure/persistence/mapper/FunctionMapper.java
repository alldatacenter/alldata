package com.hw.lineage.server.infrastructure.persistence.mapper;

import static com.hw.lineage.server.infrastructure.persistence.mapper.FunctionDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;

import com.hw.lineage.server.infrastructure.persistence.dos.FunctionDO;
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
public interface FunctionMapper extends CommonCountMapper, CommonDeleteMapper, CommonUpdateMapper {
    BasicColumn[] selectList = BasicColumn.columnList(functionId, catalogId, functionName, database, invocation, functionPath, className, descr, createUserId, modifyUserId, createTime, modifyTime, invalid);

    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="row.functionId", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<FunctionDO> insertStatement);

    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @Results(id="FunctionDOResult", value = {
        @Result(column="function_id", property="functionId", jdbcType=JdbcType.BIGINT, id=true),
        @Result(column="catalog_id", property="catalogId", jdbcType=JdbcType.BIGINT),
        @Result(column="function_name", property="functionName", jdbcType=JdbcType.VARCHAR),
        @Result(column="database", property="database", jdbcType=JdbcType.VARCHAR),
        @Result(column="invocation", property="invocation", jdbcType=JdbcType.VARCHAR),
        @Result(column="function_path", property="functionPath", jdbcType=JdbcType.VARCHAR),
        @Result(column="class_name", property="className", jdbcType=JdbcType.VARCHAR),
        @Result(column="descr", property="descr", jdbcType=JdbcType.VARCHAR),
        @Result(column="create_user_id", property="createUserId", jdbcType=JdbcType.BIGINT),
        @Result(column="modify_user_id", property="modifyUserId", jdbcType=JdbcType.BIGINT),
        @Result(column="create_time", property="createTime", jdbcType=JdbcType.BIGINT),
        @Result(column="modify_time", property="modifyTime", jdbcType=JdbcType.BIGINT),
        @Result(column="invalid", property="invalid", jdbcType=JdbcType.BIT)
    })
    List<FunctionDO> selectMany(SelectStatementProvider selectStatement);

    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ResultMap("FunctionDOResult")
    Optional<FunctionDO> selectOne(SelectStatementProvider selectStatement);

    default long count(CountDSLCompleter completer) {
        return MyBatis3Utils.countFrom(this::count, function, completer);
    }

    default int delete(DeleteDSLCompleter completer) {
        return MyBatis3Utils.deleteFrom(this::delete, function, completer);
    }

    default int deleteByPrimaryKey(Long functionId_) {
        return delete(c -> 
            c.where(functionId, isEqualTo(functionId_))
        );
    }

    default int insert(FunctionDO row) {
        return MyBatis3Utils.insert(this::insert, row, function, c ->
            c.map(catalogId).toProperty("catalogId")
            .map(functionName).toProperty("functionName")
            .map(database).toProperty("database")
            .map(invocation).toProperty("invocation")
            .map(functionPath).toProperty("functionPath")
            .map(className).toProperty("className")
            .map(descr).toProperty("descr")
            .map(createUserId).toProperty("createUserId")
            .map(modifyUserId).toProperty("modifyUserId")
            .map(createTime).toProperty("createTime")
            .map(modifyTime).toProperty("modifyTime")
            .map(invalid).toProperty("invalid")
        );
    }

    default int insertSelective(FunctionDO row) {
        return MyBatis3Utils.insert(this::insert, row, function, c ->
            c.map(catalogId).toPropertyWhenPresent("catalogId", row::getCatalogId)
            .map(functionName).toPropertyWhenPresent("functionName", row::getFunctionName)
            .map(database).toPropertyWhenPresent("database", row::getDatabase)
            .map(invocation).toPropertyWhenPresent("invocation", row::getInvocation)
            .map(functionPath).toPropertyWhenPresent("functionPath", row::getFunctionPath)
            .map(className).toPropertyWhenPresent("className", row::getClassName)
            .map(descr).toPropertyWhenPresent("descr", row::getDescr)
            .map(createUserId).toPropertyWhenPresent("createUserId", row::getCreateUserId)
            .map(modifyUserId).toPropertyWhenPresent("modifyUserId", row::getModifyUserId)
            .map(createTime).toPropertyWhenPresent("createTime", row::getCreateTime)
            .map(modifyTime).toPropertyWhenPresent("modifyTime", row::getModifyTime)
            .map(invalid).toPropertyWhenPresent("invalid", row::getInvalid)
        );
    }

    default Optional<FunctionDO> selectOne(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectOne(this::selectOne, selectList, function, completer);
    }

    default List<FunctionDO> select(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectList(this::selectMany, selectList, function, completer);
    }

    default List<FunctionDO> selectDistinct(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectDistinct(this::selectMany, selectList, function, completer);
    }

    default Optional<FunctionDO> selectByPrimaryKey(Long functionId_) {
        return selectOne(c ->
            c.where(functionId, isEqualTo(functionId_))
        );
    }

    default int update(UpdateDSLCompleter completer) {
        return MyBatis3Utils.update(this::update, function, completer);
    }

    static UpdateDSL<UpdateModel> updateAllColumns(FunctionDO row, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(catalogId).equalTo(row::getCatalogId)
                .set(functionName).equalTo(row::getFunctionName)
                .set(database).equalTo(row::getDatabase)
                .set(invocation).equalTo(row::getInvocation)
                .set(functionPath).equalTo(row::getFunctionPath)
                .set(className).equalTo(row::getClassName)
                .set(descr).equalTo(row::getDescr)
                .set(createUserId).equalTo(row::getCreateUserId)
                .set(modifyUserId).equalTo(row::getModifyUserId)
                .set(createTime).equalTo(row::getCreateTime)
                .set(modifyTime).equalTo(row::getModifyTime)
                .set(invalid).equalTo(row::getInvalid);
    }

    static UpdateDSL<UpdateModel> updateSelectiveColumns(FunctionDO row, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(catalogId).equalToWhenPresent(row::getCatalogId)
                .set(functionName).equalToWhenPresent(row::getFunctionName)
                .set(database).equalToWhenPresent(row::getDatabase)
                .set(invocation).equalToWhenPresent(row::getInvocation)
                .set(functionPath).equalToWhenPresent(row::getFunctionPath)
                .set(className).equalToWhenPresent(row::getClassName)
                .set(descr).equalToWhenPresent(row::getDescr)
                .set(createUserId).equalToWhenPresent(row::getCreateUserId)
                .set(modifyUserId).equalToWhenPresent(row::getModifyUserId)
                .set(createTime).equalToWhenPresent(row::getCreateTime)
                .set(modifyTime).equalToWhenPresent(row::getModifyTime)
                .set(invalid).equalToWhenPresent(row::getInvalid);
    }

    default int updateByPrimaryKey(FunctionDO row) {
        return update(c ->
            c.set(catalogId).equalTo(row::getCatalogId)
            .set(functionName).equalTo(row::getFunctionName)
            .set(database).equalTo(row::getDatabase)
            .set(invocation).equalTo(row::getInvocation)
            .set(functionPath).equalTo(row::getFunctionPath)
            .set(className).equalTo(row::getClassName)
            .set(descr).equalTo(row::getDescr)
            .set(createUserId).equalTo(row::getCreateUserId)
            .set(modifyUserId).equalTo(row::getModifyUserId)
            .set(createTime).equalTo(row::getCreateTime)
            .set(modifyTime).equalTo(row::getModifyTime)
            .set(invalid).equalTo(row::getInvalid)
            .where(functionId, isEqualTo(row::getFunctionId))
        );
    }

    default int updateByPrimaryKeySelective(FunctionDO row) {
        return update(c ->
            c.set(catalogId).equalToWhenPresent(row::getCatalogId)
            .set(functionName).equalToWhenPresent(row::getFunctionName)
            .set(database).equalToWhenPresent(row::getDatabase)
            .set(invocation).equalToWhenPresent(row::getInvocation)
            .set(functionPath).equalToWhenPresent(row::getFunctionPath)
            .set(className).equalToWhenPresent(row::getClassName)
            .set(descr).equalToWhenPresent(row::getDescr)
            .set(createUserId).equalToWhenPresent(row::getCreateUserId)
            .set(modifyUserId).equalToWhenPresent(row::getModifyUserId)
            .set(createTime).equalToWhenPresent(row::getCreateTime)
            .set(modifyTime).equalToWhenPresent(row::getModifyTime)
            .set(invalid).equalToWhenPresent(row::getInvalid)
            .where(functionId, isEqualTo(row::getFunctionId))
        );
    }
}
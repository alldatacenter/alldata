package com.hw.lineage.server.infrastructure.persistence.mapper;

import static com.hw.lineage.server.infrastructure.persistence.mapper.PluginDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;

import com.hw.lineage.server.infrastructure.persistence.dos.PluginDO;
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
public interface PluginMapper extends CommonCountMapper, CommonDeleteMapper, CommonUpdateMapper {
    BasicColumn[] selectList = BasicColumn.columnList(pluginId, pluginName, pluginCode, descr, defaultPlugin, createUserId, modifyUserId, createTime, modifyTime, invalid);

    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="row.pluginId", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<PluginDO> insertStatement);

    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @Results(id="PluginDOResult", value = {
        @Result(column="plugin_id", property="pluginId", jdbcType=JdbcType.BIGINT, id=true),
        @Result(column="plugin_name", property="pluginName", jdbcType=JdbcType.VARCHAR),
        @Result(column="plugin_code", property="pluginCode", jdbcType=JdbcType.VARCHAR),
        @Result(column="descr", property="descr", jdbcType=JdbcType.VARCHAR),
        @Result(column="default_plugin", property="defaultPlugin", jdbcType=JdbcType.BIT),
        @Result(column="create_user_id", property="createUserId", jdbcType=JdbcType.BIGINT),
        @Result(column="modify_user_id", property="modifyUserId", jdbcType=JdbcType.BIGINT),
        @Result(column="create_time", property="createTime", jdbcType=JdbcType.BIGINT),
        @Result(column="modify_time", property="modifyTime", jdbcType=JdbcType.BIGINT),
        @Result(column="invalid", property="invalid", jdbcType=JdbcType.BIT)
    })
    List<PluginDO> selectMany(SelectStatementProvider selectStatement);

    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ResultMap("PluginDOResult")
    Optional<PluginDO> selectOne(SelectStatementProvider selectStatement);

    default long count(CountDSLCompleter completer) {
        return MyBatis3Utils.countFrom(this::count, plugin, completer);
    }

    default int delete(DeleteDSLCompleter completer) {
        return MyBatis3Utils.deleteFrom(this::delete, plugin, completer);
    }

    default int deleteByPrimaryKey(Long pluginId_) {
        return delete(c -> 
            c.where(pluginId, isEqualTo(pluginId_))
        );
    }

    default int insert(PluginDO row) {
        return MyBatis3Utils.insert(this::insert, row, plugin, c ->
            c.map(pluginName).toProperty("pluginName")
            .map(pluginCode).toProperty("pluginCode")
            .map(descr).toProperty("descr")
            .map(defaultPlugin).toProperty("defaultPlugin")
            .map(createUserId).toProperty("createUserId")
            .map(modifyUserId).toProperty("modifyUserId")
            .map(createTime).toProperty("createTime")
            .map(modifyTime).toProperty("modifyTime")
            .map(invalid).toProperty("invalid")
        );
    }

    default int insertSelective(PluginDO row) {
        return MyBatis3Utils.insert(this::insert, row, plugin, c ->
            c.map(pluginName).toPropertyWhenPresent("pluginName", row::getPluginName)
            .map(pluginCode).toPropertyWhenPresent("pluginCode", row::getPluginCode)
            .map(descr).toPropertyWhenPresent("descr", row::getDescr)
            .map(defaultPlugin).toPropertyWhenPresent("defaultPlugin", row::getDefaultPlugin)
            .map(createUserId).toPropertyWhenPresent("createUserId", row::getCreateUserId)
            .map(modifyUserId).toPropertyWhenPresent("modifyUserId", row::getModifyUserId)
            .map(createTime).toPropertyWhenPresent("createTime", row::getCreateTime)
            .map(modifyTime).toPropertyWhenPresent("modifyTime", row::getModifyTime)
            .map(invalid).toPropertyWhenPresent("invalid", row::getInvalid)
        );
    }

    default Optional<PluginDO> selectOne(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectOne(this::selectOne, selectList, plugin, completer);
    }

    default List<PluginDO> select(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectList(this::selectMany, selectList, plugin, completer);
    }

    default List<PluginDO> selectDistinct(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectDistinct(this::selectMany, selectList, plugin, completer);
    }

    default Optional<PluginDO> selectByPrimaryKey(Long pluginId_) {
        return selectOne(c ->
            c.where(pluginId, isEqualTo(pluginId_))
        );
    }

    default int update(UpdateDSLCompleter completer) {
        return MyBatis3Utils.update(this::update, plugin, completer);
    }

    static UpdateDSL<UpdateModel> updateAllColumns(PluginDO row, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(pluginName).equalTo(row::getPluginName)
                .set(pluginCode).equalTo(row::getPluginCode)
                .set(descr).equalTo(row::getDescr)
                .set(defaultPlugin).equalTo(row::getDefaultPlugin)
                .set(createUserId).equalTo(row::getCreateUserId)
                .set(modifyUserId).equalTo(row::getModifyUserId)
                .set(createTime).equalTo(row::getCreateTime)
                .set(modifyTime).equalTo(row::getModifyTime)
                .set(invalid).equalTo(row::getInvalid);
    }

    static UpdateDSL<UpdateModel> updateSelectiveColumns(PluginDO row, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(pluginName).equalToWhenPresent(row::getPluginName)
                .set(pluginCode).equalToWhenPresent(row::getPluginCode)
                .set(descr).equalToWhenPresent(row::getDescr)
                .set(defaultPlugin).equalToWhenPresent(row::getDefaultPlugin)
                .set(createUserId).equalToWhenPresent(row::getCreateUserId)
                .set(modifyUserId).equalToWhenPresent(row::getModifyUserId)
                .set(createTime).equalToWhenPresent(row::getCreateTime)
                .set(modifyTime).equalToWhenPresent(row::getModifyTime)
                .set(invalid).equalToWhenPresent(row::getInvalid);
    }

    default int updateByPrimaryKey(PluginDO row) {
        return update(c ->
            c.set(pluginName).equalTo(row::getPluginName)
            .set(pluginCode).equalTo(row::getPluginCode)
            .set(descr).equalTo(row::getDescr)
            .set(defaultPlugin).equalTo(row::getDefaultPlugin)
            .set(createUserId).equalTo(row::getCreateUserId)
            .set(modifyUserId).equalTo(row::getModifyUserId)
            .set(createTime).equalTo(row::getCreateTime)
            .set(modifyTime).equalTo(row::getModifyTime)
            .set(invalid).equalTo(row::getInvalid)
            .where(pluginId, isEqualTo(row::getPluginId))
        );
    }

    default int updateByPrimaryKeySelective(PluginDO row) {
        return update(c ->
            c.set(pluginName).equalToWhenPresent(row::getPluginName)
            .set(pluginCode).equalToWhenPresent(row::getPluginCode)
            .set(descr).equalToWhenPresent(row::getDescr)
            .set(defaultPlugin).equalToWhenPresent(row::getDefaultPlugin)
            .set(createUserId).equalToWhenPresent(row::getCreateUserId)
            .set(modifyUserId).equalToWhenPresent(row::getModifyUserId)
            .set(createTime).equalToWhenPresent(row::getCreateTime)
            .set(modifyTime).equalToWhenPresent(row::getModifyTime)
            .set(invalid).equalToWhenPresent(row::getInvalid)
            .where(pluginId, isEqualTo(row::getPluginId))
        );
    }
}
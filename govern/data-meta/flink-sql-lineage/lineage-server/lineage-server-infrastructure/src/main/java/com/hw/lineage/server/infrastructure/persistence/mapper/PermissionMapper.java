package com.hw.lineage.server.infrastructure.persistence.mapper;

import static com.hw.lineage.server.infrastructure.persistence.mapper.PermissionDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;

import com.hw.lineage.server.infrastructure.persistence.dos.PermissionDO;
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
public interface PermissionMapper extends CommonCountMapper, CommonDeleteMapper, CommonUpdateMapper {
    BasicColumn[] selectList = BasicColumn.columnList(permissionId, permissionGroup, permissionName, permissionCode, createTime, modifyTime, invalid);

    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="row.permissionId", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<PermissionDO> insertStatement);

    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @Results(id="PermissionDOResult", value = {
        @Result(column="permission_id", property="permissionId", jdbcType=JdbcType.BIGINT, id=true),
        @Result(column="permission_group", property="permissionGroup", jdbcType=JdbcType.VARCHAR),
        @Result(column="permission_name", property="permissionName", jdbcType=JdbcType.VARCHAR),
        @Result(column="permission_code", property="permissionCode", jdbcType=JdbcType.VARCHAR),
        @Result(column="create_time", property="createTime", jdbcType=JdbcType.BIGINT),
        @Result(column="modify_time", property="modifyTime", jdbcType=JdbcType.BIGINT),
        @Result(column="invalid", property="invalid", jdbcType=JdbcType.BIT)
    })
    List<PermissionDO> selectMany(SelectStatementProvider selectStatement);

    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ResultMap("PermissionDOResult")
    Optional<PermissionDO> selectOne(SelectStatementProvider selectStatement);

    default long count(CountDSLCompleter completer) {
        return MyBatis3Utils.countFrom(this::count, permission, completer);
    }

    default int delete(DeleteDSLCompleter completer) {
        return MyBatis3Utils.deleteFrom(this::delete, permission, completer);
    }

    default int deleteByPrimaryKey(Long permissionId_) {
        return delete(c -> 
            c.where(permissionId, isEqualTo(permissionId_))
        );
    }

    default int insert(PermissionDO row) {
        return MyBatis3Utils.insert(this::insert, row, permission, c ->
            c.map(permissionGroup).toProperty("permissionGroup")
            .map(permissionName).toProperty("permissionName")
            .map(permissionCode).toProperty("permissionCode")
            .map(createTime).toProperty("createTime")
            .map(modifyTime).toProperty("modifyTime")
            .map(invalid).toProperty("invalid")
        );
    }

    default int insertSelective(PermissionDO row) {
        return MyBatis3Utils.insert(this::insert, row, permission, c ->
            c.map(permissionGroup).toPropertyWhenPresent("permissionGroup", row::getPermissionGroup)
            .map(permissionName).toPropertyWhenPresent("permissionName", row::getPermissionName)
            .map(permissionCode).toPropertyWhenPresent("permissionCode", row::getPermissionCode)
            .map(createTime).toPropertyWhenPresent("createTime", row::getCreateTime)
            .map(modifyTime).toPropertyWhenPresent("modifyTime", row::getModifyTime)
            .map(invalid).toPropertyWhenPresent("invalid", row::getInvalid)
        );
    }

    default Optional<PermissionDO> selectOne(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectOne(this::selectOne, selectList, permission, completer);
    }

    default List<PermissionDO> select(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectList(this::selectMany, selectList, permission, completer);
    }

    default List<PermissionDO> selectDistinct(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectDistinct(this::selectMany, selectList, permission, completer);
    }

    default Optional<PermissionDO> selectByPrimaryKey(Long permissionId_) {
        return selectOne(c ->
            c.where(permissionId, isEqualTo(permissionId_))
        );
    }

    default int update(UpdateDSLCompleter completer) {
        return MyBatis3Utils.update(this::update, permission, completer);
    }

    static UpdateDSL<UpdateModel> updateAllColumns(PermissionDO row, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(permissionGroup).equalTo(row::getPermissionGroup)
                .set(permissionName).equalTo(row::getPermissionName)
                .set(permissionCode).equalTo(row::getPermissionCode)
                .set(createTime).equalTo(row::getCreateTime)
                .set(modifyTime).equalTo(row::getModifyTime)
                .set(invalid).equalTo(row::getInvalid);
    }

    static UpdateDSL<UpdateModel> updateSelectiveColumns(PermissionDO row, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(permissionGroup).equalToWhenPresent(row::getPermissionGroup)
                .set(permissionName).equalToWhenPresent(row::getPermissionName)
                .set(permissionCode).equalToWhenPresent(row::getPermissionCode)
                .set(createTime).equalToWhenPresent(row::getCreateTime)
                .set(modifyTime).equalToWhenPresent(row::getModifyTime)
                .set(invalid).equalToWhenPresent(row::getInvalid);
    }

    default int updateByPrimaryKey(PermissionDO row) {
        return update(c ->
            c.set(permissionGroup).equalTo(row::getPermissionGroup)
            .set(permissionName).equalTo(row::getPermissionName)
            .set(permissionCode).equalTo(row::getPermissionCode)
            .set(createTime).equalTo(row::getCreateTime)
            .set(modifyTime).equalTo(row::getModifyTime)
            .set(invalid).equalTo(row::getInvalid)
            .where(permissionId, isEqualTo(row::getPermissionId))
        );
    }

    default int updateByPrimaryKeySelective(PermissionDO row) {
        return update(c ->
            c.set(permissionGroup).equalToWhenPresent(row::getPermissionGroup)
            .set(permissionName).equalToWhenPresent(row::getPermissionName)
            .set(permissionCode).equalToWhenPresent(row::getPermissionCode)
            .set(createTime).equalToWhenPresent(row::getCreateTime)
            .set(modifyTime).equalToWhenPresent(row::getModifyTime)
            .set(invalid).equalToWhenPresent(row::getInvalid)
            .where(permissionId, isEqualTo(row::getPermissionId))
        );
    }
}
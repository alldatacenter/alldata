package com.hw.lineage.server.infrastructure.persistence.mapper;

import static com.hw.lineage.server.infrastructure.persistence.mapper.RoleDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;

import com.hw.lineage.server.infrastructure.persistence.dos.RoleDO;
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
public interface RoleMapper extends CommonCountMapper, CommonDeleteMapper, CommonUpdateMapper {
    BasicColumn[] selectList = BasicColumn.columnList(roleId, roleName, createTime, modifyTime, invalid);

    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="row.roleId", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<RoleDO> insertStatement);

    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @Results(id="RoleDOResult", value = {
        @Result(column="role_id", property="roleId", jdbcType=JdbcType.BIGINT, id=true),
        @Result(column="role_name", property="roleName", jdbcType=JdbcType.VARCHAR),
        @Result(column="create_time", property="createTime", jdbcType=JdbcType.BIGINT),
        @Result(column="modify_time", property="modifyTime", jdbcType=JdbcType.BIGINT),
        @Result(column="invalid", property="invalid", jdbcType=JdbcType.BIT)
    })
    List<RoleDO> selectMany(SelectStatementProvider selectStatement);

    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ResultMap("RoleDOResult")
    Optional<RoleDO> selectOne(SelectStatementProvider selectStatement);

    default long count(CountDSLCompleter completer) {
        return MyBatis3Utils.countFrom(this::count, role, completer);
    }

    default int delete(DeleteDSLCompleter completer) {
        return MyBatis3Utils.deleteFrom(this::delete, role, completer);
    }

    default int deleteByPrimaryKey(Long roleId_) {
        return delete(c -> 
            c.where(roleId, isEqualTo(roleId_))
        );
    }

    default int insert(RoleDO row) {
        return MyBatis3Utils.insert(this::insert, row, role, c ->
            c.map(roleName).toProperty("roleName")
            .map(createTime).toProperty("createTime")
            .map(modifyTime).toProperty("modifyTime")
            .map(invalid).toProperty("invalid")
        );
    }

    default int insertSelective(RoleDO row) {
        return MyBatis3Utils.insert(this::insert, row, role, c ->
            c.map(roleName).toPropertyWhenPresent("roleName", row::getRoleName)
            .map(createTime).toPropertyWhenPresent("createTime", row::getCreateTime)
            .map(modifyTime).toPropertyWhenPresent("modifyTime", row::getModifyTime)
            .map(invalid).toPropertyWhenPresent("invalid", row::getInvalid)
        );
    }

    default Optional<RoleDO> selectOne(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectOne(this::selectOne, selectList, role, completer);
    }

    default List<RoleDO> select(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectList(this::selectMany, selectList, role, completer);
    }

    default List<RoleDO> selectDistinct(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectDistinct(this::selectMany, selectList, role, completer);
    }

    default Optional<RoleDO> selectByPrimaryKey(Long roleId_) {
        return selectOne(c ->
            c.where(roleId, isEqualTo(roleId_))
        );
    }

    default int update(UpdateDSLCompleter completer) {
        return MyBatis3Utils.update(this::update, role, completer);
    }

    static UpdateDSL<UpdateModel> updateAllColumns(RoleDO row, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(roleName).equalTo(row::getRoleName)
                .set(createTime).equalTo(row::getCreateTime)
                .set(modifyTime).equalTo(row::getModifyTime)
                .set(invalid).equalTo(row::getInvalid);
    }

    static UpdateDSL<UpdateModel> updateSelectiveColumns(RoleDO row, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(roleName).equalToWhenPresent(row::getRoleName)
                .set(createTime).equalToWhenPresent(row::getCreateTime)
                .set(modifyTime).equalToWhenPresent(row::getModifyTime)
                .set(invalid).equalToWhenPresent(row::getInvalid);
    }

    default int updateByPrimaryKey(RoleDO row) {
        return update(c ->
            c.set(roleName).equalTo(row::getRoleName)
            .set(createTime).equalTo(row::getCreateTime)
            .set(modifyTime).equalTo(row::getModifyTime)
            .set(invalid).equalTo(row::getInvalid)
            .where(roleId, isEqualTo(row::getRoleId))
        );
    }

    default int updateByPrimaryKeySelective(RoleDO row) {
        return update(c ->
            c.set(roleName).equalToWhenPresent(row::getRoleName)
            .set(createTime).equalToWhenPresent(row::getCreateTime)
            .set(modifyTime).equalToWhenPresent(row::getModifyTime)
            .set(invalid).equalToWhenPresent(row::getInvalid)
            .where(roleId, isEqualTo(row::getRoleId))
        );
    }
}
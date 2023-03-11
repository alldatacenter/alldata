package com.hw.lineage.server.infrastructure.persistence.mapper;

import static com.hw.lineage.server.infrastructure.persistence.mapper.RolePermissionDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;

import com.hw.lineage.server.infrastructure.persistence.dos.RolePermissionDO;
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
public interface RolePermissionMapper extends CommonCountMapper, CommonDeleteMapper, CommonInsertMapper<RolePermissionDO>, CommonUpdateMapper {
    BasicColumn[] selectList = BasicColumn.columnList(rid, roleId, permissionId, invalid);

    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @Results(id="RolePermissionDOResult", value = {
        @Result(column="rid", property="rid", jdbcType=JdbcType.BIGINT, id=true),
        @Result(column="role_id", property="roleId", jdbcType=JdbcType.BIGINT),
        @Result(column="permission_id", property="permissionId", jdbcType=JdbcType.BIGINT),
        @Result(column="invalid", property="invalid", jdbcType=JdbcType.BIT)
    })
    List<RolePermissionDO> selectMany(SelectStatementProvider selectStatement);

    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ResultMap("RolePermissionDOResult")
    Optional<RolePermissionDO> selectOne(SelectStatementProvider selectStatement);

    default long count(CountDSLCompleter completer) {
        return MyBatis3Utils.countFrom(this::count, rolePermission, completer);
    }

    default int delete(DeleteDSLCompleter completer) {
        return MyBatis3Utils.deleteFrom(this::delete, rolePermission, completer);
    }

    default int deleteByPrimaryKey(Long rid_) {
        return delete(c -> 
            c.where(rid, isEqualTo(rid_))
        );
    }

    default int insert(RolePermissionDO row) {
        return MyBatis3Utils.insert(this::insert, row, rolePermission, c ->
            c.map(rid).toProperty("rid")
            .map(roleId).toProperty("roleId")
            .map(permissionId).toProperty("permissionId")
            .map(invalid).toProperty("invalid")
        );
    }

    default int insertMultiple(Collection<RolePermissionDO> records) {
        return MyBatis3Utils.insertMultiple(this::insertMultiple, records, rolePermission, c ->
            c.map(rid).toProperty("rid")
            .map(roleId).toProperty("roleId")
            .map(permissionId).toProperty("permissionId")
            .map(invalid).toProperty("invalid")
        );
    }

    default int insertSelective(RolePermissionDO row) {
        return MyBatis3Utils.insert(this::insert, row, rolePermission, c ->
            c.map(rid).toPropertyWhenPresent("rid", row::getRid)
            .map(roleId).toPropertyWhenPresent("roleId", row::getRoleId)
            .map(permissionId).toPropertyWhenPresent("permissionId", row::getPermissionId)
            .map(invalid).toPropertyWhenPresent("invalid", row::getInvalid)
        );
    }

    default Optional<RolePermissionDO> selectOne(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectOne(this::selectOne, selectList, rolePermission, completer);
    }

    default List<RolePermissionDO> select(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectList(this::selectMany, selectList, rolePermission, completer);
    }

    default List<RolePermissionDO> selectDistinct(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectDistinct(this::selectMany, selectList, rolePermission, completer);
    }

    default Optional<RolePermissionDO> selectByPrimaryKey(Long rid_) {
        return selectOne(c ->
            c.where(rid, isEqualTo(rid_))
        );
    }

    default int update(UpdateDSLCompleter completer) {
        return MyBatis3Utils.update(this::update, rolePermission, completer);
    }

    static UpdateDSL<UpdateModel> updateAllColumns(RolePermissionDO row, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(rid).equalTo(row::getRid)
                .set(roleId).equalTo(row::getRoleId)
                .set(permissionId).equalTo(row::getPermissionId)
                .set(invalid).equalTo(row::getInvalid);
    }

    static UpdateDSL<UpdateModel> updateSelectiveColumns(RolePermissionDO row, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(rid).equalToWhenPresent(row::getRid)
                .set(roleId).equalToWhenPresent(row::getRoleId)
                .set(permissionId).equalToWhenPresent(row::getPermissionId)
                .set(invalid).equalToWhenPresent(row::getInvalid);
    }

    default int updateByPrimaryKey(RolePermissionDO row) {
        return update(c ->
            c.set(roleId).equalTo(row::getRoleId)
            .set(permissionId).equalTo(row::getPermissionId)
            .set(invalid).equalTo(row::getInvalid)
            .where(rid, isEqualTo(row::getRid))
        );
    }

    default int updateByPrimaryKeySelective(RolePermissionDO row) {
        return update(c ->
            c.set(roleId).equalToWhenPresent(row::getRoleId)
            .set(permissionId).equalToWhenPresent(row::getPermissionId)
            .set(invalid).equalToWhenPresent(row::getInvalid)
            .where(rid, isEqualTo(row::getRid))
        );
    }
}
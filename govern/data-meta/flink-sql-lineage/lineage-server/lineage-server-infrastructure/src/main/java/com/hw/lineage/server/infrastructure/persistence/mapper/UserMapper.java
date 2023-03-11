package com.hw.lineage.server.infrastructure.persistence.mapper;

import static com.hw.lineage.server.infrastructure.persistence.mapper.UserDynamicSqlSupport.*;
import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;

import com.hw.lineage.server.infrastructure.persistence.dos.UserDO;
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
public interface UserMapper extends CommonCountMapper, CommonDeleteMapper, CommonUpdateMapper {
    BasicColumn[] selectList = BasicColumn.columnList(userId, username, password, locked, createTime, modifyTime, invalid);

    @InsertProvider(type=SqlProviderAdapter.class, method="insert")
    @SelectKey(statement="SELECT LAST_INSERT_ID()", keyProperty="row.userId", before=false, resultType=Long.class)
    int insert(InsertStatementProvider<UserDO> insertStatement);

    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @Results(id="UserDOResult", value = {
        @Result(column="user_id", property="userId", jdbcType=JdbcType.BIGINT, id=true),
        @Result(column="username", property="username", jdbcType=JdbcType.VARCHAR),
        @Result(column="password", property="password", jdbcType=JdbcType.VARCHAR),
        @Result(column="locked", property="locked", jdbcType=JdbcType.BIT),
        @Result(column="create_time", property="createTime", jdbcType=JdbcType.BIGINT),
        @Result(column="modify_time", property="modifyTime", jdbcType=JdbcType.BIGINT),
        @Result(column="invalid", property="invalid", jdbcType=JdbcType.BIT)
    })
    List<UserDO> selectMany(SelectStatementProvider selectStatement);

    @SelectProvider(type=SqlProviderAdapter.class, method="select")
    @ResultMap("UserDOResult")
    Optional<UserDO> selectOne(SelectStatementProvider selectStatement);

    default long count(CountDSLCompleter completer) {
        return MyBatis3Utils.countFrom(this::count, user, completer);
    }

    default int delete(DeleteDSLCompleter completer) {
        return MyBatis3Utils.deleteFrom(this::delete, user, completer);
    }

    default int deleteByPrimaryKey(Long userId_) {
        return delete(c -> 
            c.where(userId, isEqualTo(userId_))
        );
    }

    default int insert(UserDO row) {
        return MyBatis3Utils.insert(this::insert, row, user, c ->
            c.map(username).toProperty("username")
            .map(password).toProperty("password")
            .map(locked).toProperty("locked")
            .map(createTime).toProperty("createTime")
            .map(modifyTime).toProperty("modifyTime")
            .map(invalid).toProperty("invalid")
        );
    }

    default int insertSelective(UserDO row) {
        return MyBatis3Utils.insert(this::insert, row, user, c ->
            c.map(username).toPropertyWhenPresent("username", row::getUsername)
            .map(password).toPropertyWhenPresent("password", row::getPassword)
            .map(locked).toPropertyWhenPresent("locked", row::getLocked)
            .map(createTime).toPropertyWhenPresent("createTime", row::getCreateTime)
            .map(modifyTime).toPropertyWhenPresent("modifyTime", row::getModifyTime)
            .map(invalid).toPropertyWhenPresent("invalid", row::getInvalid)
        );
    }

    default Optional<UserDO> selectOne(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectOne(this::selectOne, selectList, user, completer);
    }

    default List<UserDO> select(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectList(this::selectMany, selectList, user, completer);
    }

    default List<UserDO> selectDistinct(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectDistinct(this::selectMany, selectList, user, completer);
    }

    default Optional<UserDO> selectByPrimaryKey(Long userId_) {
        return selectOne(c ->
            c.where(userId, isEqualTo(userId_))
        );
    }

    default int update(UpdateDSLCompleter completer) {
        return MyBatis3Utils.update(this::update, user, completer);
    }

    static UpdateDSL<UpdateModel> updateAllColumns(UserDO row, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(username).equalTo(row::getUsername)
                .set(password).equalTo(row::getPassword)
                .set(locked).equalTo(row::getLocked)
                .set(createTime).equalTo(row::getCreateTime)
                .set(modifyTime).equalTo(row::getModifyTime)
                .set(invalid).equalTo(row::getInvalid);
    }

    static UpdateDSL<UpdateModel> updateSelectiveColumns(UserDO row, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(username).equalToWhenPresent(row::getUsername)
                .set(password).equalToWhenPresent(row::getPassword)
                .set(locked).equalToWhenPresent(row::getLocked)
                .set(createTime).equalToWhenPresent(row::getCreateTime)
                .set(modifyTime).equalToWhenPresent(row::getModifyTime)
                .set(invalid).equalToWhenPresent(row::getInvalid);
    }

    default int updateByPrimaryKey(UserDO row) {
        return update(c ->
            c.set(username).equalTo(row::getUsername)
            .set(password).equalTo(row::getPassword)
            .set(locked).equalTo(row::getLocked)
            .set(createTime).equalTo(row::getCreateTime)
            .set(modifyTime).equalTo(row::getModifyTime)
            .set(invalid).equalTo(row::getInvalid)
            .where(userId, isEqualTo(row::getUserId))
        );
    }

    default int updateByPrimaryKeySelective(UserDO row) {
        return update(c ->
            c.set(username).equalToWhenPresent(row::getUsername)
            .set(password).equalToWhenPresent(row::getPassword)
            .set(locked).equalToWhenPresent(row::getLocked)
            .set(createTime).equalToWhenPresent(row::getCreateTime)
            .set(modifyTime).equalToWhenPresent(row::getModifyTime)
            .set(invalid).equalToWhenPresent(row::getInvalid)
            .where(userId, isEqualTo(row::getUserId))
        );
    }
}
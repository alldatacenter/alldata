package datart.core.mappers;

import datart.core.entity.AccessLog;
import datart.core.mappers.ext.CRUDMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.UpdateProvider;
import org.apache.ibatis.type.JdbcType;

public interface AccessLogMapper extends CRUDMapper {
    @Delete({
        "delete from access_log",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int deleteByPrimaryKey(String id);

    @Insert({
        "insert into access_log (id, `user`, ",
        "resource_type, resource_id, ",
        "access_type, access_time, ",
        "duration)",
        "values (#{id,jdbcType=VARCHAR}, #{user,jdbcType=VARCHAR}, ",
        "#{resourceType,jdbcType=VARCHAR}, #{resourceId,jdbcType=VARCHAR}, ",
        "#{accessType,jdbcType=VARCHAR}, #{accessTime,jdbcType=TIMESTAMP}, ",
        "#{duration,jdbcType=INTEGER})"
    })
    int insert(AccessLog record);

    @InsertProvider(type=AccessLogSqlProvider.class, method="insertSelective")
    int insertSelective(AccessLog record);

    @Select({
        "select",
        "id, `user`, resource_type, resource_id, access_type, access_time, duration",
        "from access_log",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    @Results({
        @Result(column="id", property="id", jdbcType=JdbcType.VARCHAR, id=true),
        @Result(column="user", property="user", jdbcType=JdbcType.VARCHAR),
        @Result(column="resource_type", property="resourceType", jdbcType=JdbcType.VARCHAR),
        @Result(column="resource_id", property="resourceId", jdbcType=JdbcType.VARCHAR),
        @Result(column="access_type", property="accessType", jdbcType=JdbcType.VARCHAR),
        @Result(column="access_time", property="accessTime", jdbcType=JdbcType.TIMESTAMP),
        @Result(column="duration", property="duration", jdbcType=JdbcType.INTEGER)
    })
    AccessLog selectByPrimaryKey(String id);

    @UpdateProvider(type=AccessLogSqlProvider.class, method="updateByPrimaryKeySelective")
    int updateByPrimaryKeySelective(AccessLog record);

    @Update({
        "update access_log",
        "set `user` = #{user,jdbcType=VARCHAR},",
          "resource_type = #{resourceType,jdbcType=VARCHAR},",
          "resource_id = #{resourceId,jdbcType=VARCHAR},",
          "access_type = #{accessType,jdbcType=VARCHAR},",
          "access_time = #{accessTime,jdbcType=TIMESTAMP},",
          "duration = #{duration,jdbcType=INTEGER}",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int updateByPrimaryKey(AccessLog record);
}
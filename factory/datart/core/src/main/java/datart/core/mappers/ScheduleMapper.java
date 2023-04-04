package datart.core.mappers;

import datart.core.entity.Schedule;
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

public interface ScheduleMapper extends CRUDMapper {
    @Delete({
        "delete from schedule",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int deleteByPrimaryKey(String id);

    @Insert({
        "insert into schedule (id, `name`, ",
        "org_id, `type`, active, ",
        "cron_expression, start_date, ",
        "end_date, config, ",
        "create_time, create_by, ",
        "update_by, update_time, ",
        "parent_id, is_folder, ",
        "`index`, `status`)",
        "values (#{id,jdbcType=VARCHAR}, #{name,jdbcType=VARCHAR}, ",
        "#{orgId,jdbcType=VARCHAR}, #{type,jdbcType=VARCHAR}, #{active,jdbcType=TINYINT}, ",
        "#{cronExpression,jdbcType=VARCHAR}, #{startDate,jdbcType=TIMESTAMP}, ",
        "#{endDate,jdbcType=TIMESTAMP}, #{config,jdbcType=VARCHAR}, ",
        "#{createTime,jdbcType=TIMESTAMP}, #{createBy,jdbcType=VARCHAR}, ",
        "#{updateBy,jdbcType=VARCHAR}, #{updateTime,jdbcType=TIMESTAMP}, ",
        "#{parentId,jdbcType=VARCHAR}, #{isFolder,jdbcType=TINYINT}, ",
        "#{index,jdbcType=DOUBLE}, #{status,jdbcType=TINYINT})"
    })
    int insert(Schedule record);

    @InsertProvider(type=ScheduleSqlProvider.class, method="insertSelective")
    int insertSelective(Schedule record);

    @Select({
        "select",
        "id, `name`, org_id, `type`, active, cron_expression, start_date, end_date, config, ",
        "create_time, create_by, update_by, update_time, parent_id, is_folder, `index`, ",
        "`status`",
        "from schedule",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    @Results({
        @Result(column="id", property="id", jdbcType=JdbcType.VARCHAR, id=true),
        @Result(column="name", property="name", jdbcType=JdbcType.VARCHAR),
        @Result(column="org_id", property="orgId", jdbcType=JdbcType.VARCHAR),
        @Result(column="type", property="type", jdbcType=JdbcType.VARCHAR),
        @Result(column="active", property="active", jdbcType=JdbcType.TINYINT),
        @Result(column="cron_expression", property="cronExpression", jdbcType=JdbcType.VARCHAR),
        @Result(column="start_date", property="startDate", jdbcType=JdbcType.TIMESTAMP),
        @Result(column="end_date", property="endDate", jdbcType=JdbcType.TIMESTAMP),
        @Result(column="config", property="config", jdbcType=JdbcType.VARCHAR),
        @Result(column="create_time", property="createTime", jdbcType=JdbcType.TIMESTAMP),
        @Result(column="create_by", property="createBy", jdbcType=JdbcType.VARCHAR),
        @Result(column="update_by", property="updateBy", jdbcType=JdbcType.VARCHAR),
        @Result(column="update_time", property="updateTime", jdbcType=JdbcType.TIMESTAMP),
        @Result(column="parent_id", property="parentId", jdbcType=JdbcType.VARCHAR),
        @Result(column="is_folder", property="isFolder", jdbcType=JdbcType.TINYINT),
        @Result(column="index", property="index", jdbcType=JdbcType.DOUBLE),
        @Result(column="status", property="status", jdbcType=JdbcType.TINYINT)
    })
    Schedule selectByPrimaryKey(String id);

    @UpdateProvider(type=ScheduleSqlProvider.class, method="updateByPrimaryKeySelective")
    int updateByPrimaryKeySelective(Schedule record);

    @Update({
        "update schedule",
        "set `name` = #{name,jdbcType=VARCHAR},",
          "org_id = #{orgId,jdbcType=VARCHAR},",
          "`type` = #{type,jdbcType=VARCHAR},",
          "active = #{active,jdbcType=TINYINT},",
          "cron_expression = #{cronExpression,jdbcType=VARCHAR},",
          "start_date = #{startDate,jdbcType=TIMESTAMP},",
          "end_date = #{endDate,jdbcType=TIMESTAMP},",
          "config = #{config,jdbcType=VARCHAR},",
          "create_time = #{createTime,jdbcType=TIMESTAMP},",
          "create_by = #{createBy,jdbcType=VARCHAR},",
          "update_by = #{updateBy,jdbcType=VARCHAR},",
          "update_time = #{updateTime,jdbcType=TIMESTAMP},",
          "parent_id = #{parentId,jdbcType=VARCHAR},",
          "is_folder = #{isFolder,jdbcType=TINYINT},",
          "`index` = #{index,jdbcType=DOUBLE},",
          "`status` = #{status,jdbcType=TINYINT}",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int updateByPrimaryKey(Schedule record);
}
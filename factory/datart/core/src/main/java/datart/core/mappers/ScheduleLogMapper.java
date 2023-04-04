package datart.core.mappers;

import datart.core.entity.ScheduleLog;
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

public interface ScheduleLogMapper extends CRUDMapper {
    @Delete({
        "delete from schedule_log",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int deleteByPrimaryKey(String id);

    @Insert({
        "insert into schedule_log (id, schedule_id, ",
        "`start`, `end`, ",
        "`status`, message)",
        "values (#{id,jdbcType=VARCHAR}, #{scheduleId,jdbcType=VARCHAR}, ",
        "#{start,jdbcType=TIMESTAMP}, #{end,jdbcType=TIMESTAMP}, ",
        "#{status,jdbcType=INTEGER}, #{message,jdbcType=VARCHAR})"
    })
    int insert(ScheduleLog record);

    @InsertProvider(type=ScheduleLogSqlProvider.class, method="insertSelective")
    int insertSelective(ScheduleLog record);

    @Select({
        "select",
        "id, schedule_id, `start`, `end`, `status`, message",
        "from schedule_log",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    @Results({
        @Result(column="id", property="id", jdbcType=JdbcType.VARCHAR, id=true),
        @Result(column="schedule_id", property="scheduleId", jdbcType=JdbcType.VARCHAR),
        @Result(column="start", property="start", jdbcType=JdbcType.TIMESTAMP),
        @Result(column="end", property="end", jdbcType=JdbcType.TIMESTAMP),
        @Result(column="status", property="status", jdbcType=JdbcType.INTEGER),
        @Result(column="message", property="message", jdbcType=JdbcType.VARCHAR)
    })
    ScheduleLog selectByPrimaryKey(String id);

    @UpdateProvider(type=ScheduleLogSqlProvider.class, method="updateByPrimaryKeySelective")
    int updateByPrimaryKeySelective(ScheduleLog record);

    @Update({
        "update schedule_log",
        "set schedule_id = #{scheduleId,jdbcType=VARCHAR},",
          "`start` = #{start,jdbcType=TIMESTAMP},",
          "`end` = #{end,jdbcType=TIMESTAMP},",
          "`status` = #{status,jdbcType=INTEGER},",
          "message = #{message,jdbcType=VARCHAR}",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int updateByPrimaryKey(ScheduleLog record);
}
package datart.core.mappers;

import datart.core.entity.ScheduleLog;
import org.apache.ibatis.jdbc.SQL;

public class ScheduleLogSqlProvider {
    public String insertSelective(ScheduleLog record) {
        SQL sql = new SQL();
        sql.INSERT_INTO("schedule_log");
        
        if (record.getId() != null) {
            sql.VALUES("id", "#{id,jdbcType=VARCHAR}");
        }
        
        if (record.getScheduleId() != null) {
            sql.VALUES("schedule_id", "#{scheduleId,jdbcType=VARCHAR}");
        }
        
        if (record.getStart() != null) {
            sql.VALUES("`start`", "#{start,jdbcType=TIMESTAMP}");
        }
        
        if (record.getEnd() != null) {
            sql.VALUES("`end`", "#{end,jdbcType=TIMESTAMP}");
        }
        
        if (record.getStatus() != null) {
            sql.VALUES("`status`", "#{status,jdbcType=INTEGER}");
        }
        
        if (record.getMessage() != null) {
            sql.VALUES("message", "#{message,jdbcType=VARCHAR}");
        }
        
        return sql.toString();
    }

    public String updateByPrimaryKeySelective(ScheduleLog record) {
        SQL sql = new SQL();
        sql.UPDATE("schedule_log");
        
        if (record.getScheduleId() != null) {
            sql.SET("schedule_id = #{scheduleId,jdbcType=VARCHAR}");
        }
        
        if (record.getStart() != null) {
            sql.SET("`start` = #{start,jdbcType=TIMESTAMP}");
        }
        
        if (record.getEnd() != null) {
            sql.SET("`end` = #{end,jdbcType=TIMESTAMP}");
        }
        
        if (record.getStatus() != null) {
            sql.SET("`status` = #{status,jdbcType=INTEGER}");
        }
        
        if (record.getMessage() != null) {
            sql.SET("message = #{message,jdbcType=VARCHAR}");
        }
        
        sql.WHERE("id = #{id,jdbcType=VARCHAR}");
        
        return sql.toString();
    }
}
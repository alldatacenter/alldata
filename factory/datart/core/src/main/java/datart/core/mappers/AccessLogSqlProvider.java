package datart.core.mappers;

import datart.core.entity.AccessLog;
import org.apache.ibatis.jdbc.SQL;

public class AccessLogSqlProvider {
    public String insertSelective(AccessLog record) {
        SQL sql = new SQL();
        sql.INSERT_INTO("access_log");
        
        if (record.getId() != null) {
            sql.VALUES("id", "#{id,jdbcType=VARCHAR}");
        }
        
        if (record.getUser() != null) {
            sql.VALUES("`user`", "#{user,jdbcType=VARCHAR}");
        }
        
        if (record.getResourceType() != null) {
            sql.VALUES("resource_type", "#{resourceType,jdbcType=VARCHAR}");
        }
        
        if (record.getResourceId() != null) {
            sql.VALUES("resource_id", "#{resourceId,jdbcType=VARCHAR}");
        }
        
        if (record.getAccessType() != null) {
            sql.VALUES("access_type", "#{accessType,jdbcType=VARCHAR}");
        }
        
        if (record.getAccessTime() != null) {
            sql.VALUES("access_time", "#{accessTime,jdbcType=TIMESTAMP}");
        }
        
        if (record.getDuration() != null) {
            sql.VALUES("duration", "#{duration,jdbcType=INTEGER}");
        }
        
        return sql.toString();
    }

    public String updateByPrimaryKeySelective(AccessLog record) {
        SQL sql = new SQL();
        sql.UPDATE("access_log");
        
        if (record.getUser() != null) {
            sql.SET("`user` = #{user,jdbcType=VARCHAR}");
        }
        
        if (record.getResourceType() != null) {
            sql.SET("resource_type = #{resourceType,jdbcType=VARCHAR}");
        }
        
        if (record.getResourceId() != null) {
            sql.SET("resource_id = #{resourceId,jdbcType=VARCHAR}");
        }
        
        if (record.getAccessType() != null) {
            sql.SET("access_type = #{accessType,jdbcType=VARCHAR}");
        }
        
        if (record.getAccessTime() != null) {
            sql.SET("access_time = #{accessTime,jdbcType=TIMESTAMP}");
        }
        
        if (record.getDuration() != null) {
            sql.SET("duration = #{duration,jdbcType=INTEGER}");
        }
        
        sql.WHERE("id = #{id,jdbcType=VARCHAR}");
        
        return sql.toString();
    }
}
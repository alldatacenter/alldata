package datart.core.mappers;

import datart.core.entity.Dashboard;
import org.apache.ibatis.jdbc.SQL;

public class DashboardSqlProvider {
    public String insertSelective(Dashboard record) {
        SQL sql = new SQL();
        sql.INSERT_INTO("dashboard");
        
        if (record.getId() != null) {
            sql.VALUES("id", "#{id,jdbcType=VARCHAR}");
        }
        
        if (record.getName() != null) {
            sql.VALUES("`name`", "#{name,jdbcType=VARCHAR}");
        }
        
        if (record.getOrgId() != null) {
            sql.VALUES("org_id", "#{orgId,jdbcType=VARCHAR}");
        }
        
        if (record.getConfig() != null) {
            sql.VALUES("config", "#{config,jdbcType=VARCHAR}");
        }
        
        if (record.getThumbnail() != null) {
            sql.VALUES("thumbnail", "#{thumbnail,jdbcType=VARCHAR}");
        }
        
        if (record.getCreateBy() != null) {
            sql.VALUES("create_by", "#{createBy,jdbcType=VARCHAR}");
        }
        
        if (record.getCreateTime() != null) {
            sql.VALUES("create_time", "#{createTime,jdbcType=TIMESTAMP}");
        }
        
        if (record.getUpdateBy() != null) {
            sql.VALUES("update_by", "#{updateBy,jdbcType=VARCHAR}");
        }
        
        if (record.getUpdateTime() != null) {
            sql.VALUES("update_time", "#{updateTime,jdbcType=TIMESTAMP}");
        }
        
        if (record.getStatus() != null) {
            sql.VALUES("`status`", "#{status,jdbcType=TINYINT}");
        }
        
        return sql.toString();
    }

    public String updateByPrimaryKeySelective(Dashboard record) {
        SQL sql = new SQL();
        sql.UPDATE("dashboard");
        
        if (record.getName() != null) {
            sql.SET("`name` = #{name,jdbcType=VARCHAR}");
        }
        
        if (record.getOrgId() != null) {
            sql.SET("org_id = #{orgId,jdbcType=VARCHAR}");
        }
        
        if (record.getConfig() != null) {
            sql.SET("config = #{config,jdbcType=VARCHAR}");
        }
        
        if (record.getThumbnail() != null) {
            sql.SET("thumbnail = #{thumbnail,jdbcType=VARCHAR}");
        }
        
        if (record.getCreateBy() != null) {
            sql.SET("create_by = #{createBy,jdbcType=VARCHAR}");
        }
        
        if (record.getCreateTime() != null) {
            sql.SET("create_time = #{createTime,jdbcType=TIMESTAMP}");
        }
        
        if (record.getUpdateBy() != null) {
            sql.SET("update_by = #{updateBy,jdbcType=VARCHAR}");
        }
        
        if (record.getUpdateTime() != null) {
            sql.SET("update_time = #{updateTime,jdbcType=TIMESTAMP}");
        }
        
        if (record.getStatus() != null) {
            sql.SET("`status` = #{status,jdbcType=TINYINT}");
        }
        
        sql.WHERE("id = #{id,jdbcType=VARCHAR}");
        
        return sql.toString();
    }
}
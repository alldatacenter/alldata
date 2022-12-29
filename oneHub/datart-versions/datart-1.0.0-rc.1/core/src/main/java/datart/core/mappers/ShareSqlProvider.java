package datart.core.mappers;

import datart.core.entity.Share;
import org.apache.ibatis.jdbc.SQL;

public class ShareSqlProvider {
    public String insertSelective(Share record) {
        SQL sql = new SQL();
        sql.INSERT_INTO("share");
        
        if (record.getId() != null) {
            sql.VALUES("id", "#{id,jdbcType=VARCHAR}");
        }
        
        if (record.getOrgId() != null) {
            sql.VALUES("org_id", "#{orgId,jdbcType=VARCHAR}");
        }
        
        if (record.getVizType() != null) {
            sql.VALUES("viz_type", "#{vizType,jdbcType=VARCHAR}");
        }
        
        if (record.getVizId() != null) {
            sql.VALUES("viz_id", "#{vizId,jdbcType=VARCHAR}");
        }
        
        if (record.getAuthenticationMode() != null) {
            sql.VALUES("authentication_mode", "#{authenticationMode,jdbcType=VARCHAR}");
        }
        
        if (record.getRowPermissionBy() != null) {
            sql.VALUES("row_permission_by", "#{rowPermissionBy,jdbcType=VARCHAR}");
        }
        
        if (record.getAuthenticationCode() != null) {
            sql.VALUES("authentication_code", "#{authenticationCode,jdbcType=VARCHAR}");
        }
        
        if (record.getExpiryDate() != null) {
            sql.VALUES("expiry_date", "#{expiryDate,jdbcType=TIMESTAMP}");
        }
        
        if (record.getCreateBy() != null) {
            sql.VALUES("create_by", "#{createBy,jdbcType=VARCHAR}");
        }
        
        if (record.getCreateTime() != null) {
            sql.VALUES("create_time", "#{createTime,jdbcType=TIMESTAMP}");
        }
        
        if (record.getRoles() != null) {
            sql.VALUES("roles", "#{roles,jdbcType=LONGVARCHAR}");
        }
        
        return sql.toString();
    }

    public String updateByPrimaryKeySelective(Share record) {
        SQL sql = new SQL();
        sql.UPDATE("share");
        
        if (record.getOrgId() != null) {
            sql.SET("org_id = #{orgId,jdbcType=VARCHAR}");
        }
        
        if (record.getVizType() != null) {
            sql.SET("viz_type = #{vizType,jdbcType=VARCHAR}");
        }
        
        if (record.getVizId() != null) {
            sql.SET("viz_id = #{vizId,jdbcType=VARCHAR}");
        }
        
        if (record.getAuthenticationMode() != null) {
            sql.SET("authentication_mode = #{authenticationMode,jdbcType=VARCHAR}");
        }
        
        if (record.getRowPermissionBy() != null) {
            sql.SET("row_permission_by = #{rowPermissionBy,jdbcType=VARCHAR}");
        }
        
        if (record.getAuthenticationCode() != null) {
            sql.SET("authentication_code = #{authenticationCode,jdbcType=VARCHAR}");
        }
        
        if (record.getExpiryDate() != null) {
            sql.SET("expiry_date = #{expiryDate,jdbcType=TIMESTAMP}");
        }
        
        if (record.getCreateBy() != null) {
            sql.SET("create_by = #{createBy,jdbcType=VARCHAR}");
        }
        
        if (record.getCreateTime() != null) {
            sql.SET("create_time = #{createTime,jdbcType=TIMESTAMP}");
        }
        
        if (record.getRoles() != null) {
            sql.SET("roles = #{roles,jdbcType=LONGVARCHAR}");
        }
        
        sql.WHERE("id = #{id,jdbcType=VARCHAR}");
        
        return sql.toString();
    }
}
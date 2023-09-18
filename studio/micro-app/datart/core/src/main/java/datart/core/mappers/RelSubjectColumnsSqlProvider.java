package datart.core.mappers;

import datart.core.entity.RelSubjectColumns;
import org.apache.ibatis.jdbc.SQL;

public class RelSubjectColumnsSqlProvider {
    public String insertSelective(RelSubjectColumns record) {
        SQL sql = new SQL();
        sql.INSERT_INTO("rel_subject_columns");
        
        if (record.getId() != null) {
            sql.VALUES("id", "#{id,jdbcType=VARCHAR}");
        }
        
        if (record.getViewId() != null) {
            sql.VALUES("view_id", "#{viewId,jdbcType=VARCHAR}");
        }
        
        if (record.getSubjectId() != null) {
            sql.VALUES("subject_id", "#{subjectId,jdbcType=VARCHAR}");
        }
        
        if (record.getSubjectType() != null) {
            sql.VALUES("subject_type", "#{subjectType,jdbcType=VARCHAR}");
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
        
        if (record.getColumnPermission() != null) {
            sql.VALUES("column_permission", "#{columnPermission,jdbcType=LONGVARCHAR}");
        }
        
        return sql.toString();
    }

    public String updateByPrimaryKeySelective(RelSubjectColumns record) {
        SQL sql = new SQL();
        sql.UPDATE("rel_subject_columns");
        
        if (record.getViewId() != null) {
            sql.SET("view_id = #{viewId,jdbcType=VARCHAR}");
        }
        
        if (record.getSubjectId() != null) {
            sql.SET("subject_id = #{subjectId,jdbcType=VARCHAR}");
        }
        
        if (record.getSubjectType() != null) {
            sql.SET("subject_type = #{subjectType,jdbcType=VARCHAR}");
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
        
        if (record.getColumnPermission() != null) {
            sql.SET("column_permission = #{columnPermission,jdbcType=LONGVARCHAR}");
        }
        
        sql.WHERE("id = #{id,jdbcType=VARCHAR}");
        
        return sql.toString();
    }
}
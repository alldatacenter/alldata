package datart.core.mappers;

import datart.core.entity.RelVariableSubject;
import org.apache.ibatis.jdbc.SQL;

public class RelVariableSubjectSqlProvider {
    public String insertSelective(RelVariableSubject record) {
        SQL sql = new SQL();
        sql.INSERT_INTO("rel_variable_subject");
        
        if (record.getId() != null) {
            sql.VALUES("id", "#{id,jdbcType=VARCHAR}");
        }
        
        if (record.getVariableId() != null) {
            sql.VALUES("variable_id", "#{variableId,jdbcType=VARCHAR}");
        }
        
        if (record.getSubjectId() != null) {
            sql.VALUES("subject_id", "#{subjectId,jdbcType=VARCHAR}");
        }
        
        if (record.getSubjectType() != null) {
            sql.VALUES("subject_type", "#{subjectType,jdbcType=VARCHAR}");
        }
        
        if (record.getValue() != null) {
            sql.VALUES("`value`", "#{value,jdbcType=VARCHAR}");
        }
        
        if (record.getCreateTime() != null) {
            sql.VALUES("create_time", "#{createTime,jdbcType=TIMESTAMP}");
        }
        
        if (record.getUseDefaultValue() != null) {
            sql.VALUES("use_default_value", "#{useDefaultValue,jdbcType=TINYINT}");
        }
        
        if (record.getCreateBy() != null) {
            sql.VALUES("create_by", "#{createBy,jdbcType=VARCHAR}");
        }
        
        if (record.getUpdateTime() != null) {
            sql.VALUES("update_time", "#{updateTime,jdbcType=TIMESTAMP}");
        }
        
        if (record.getUpdateBy() != null) {
            sql.VALUES("update_by", "#{updateBy,jdbcType=VARCHAR}");
        }
        
        return sql.toString();
    }

    public String updateByPrimaryKeySelective(RelVariableSubject record) {
        SQL sql = new SQL();
        sql.UPDATE("rel_variable_subject");
        
        if (record.getVariableId() != null) {
            sql.SET("variable_id = #{variableId,jdbcType=VARCHAR}");
        }
        
        if (record.getSubjectId() != null) {
            sql.SET("subject_id = #{subjectId,jdbcType=VARCHAR}");
        }
        
        if (record.getSubjectType() != null) {
            sql.SET("subject_type = #{subjectType,jdbcType=VARCHAR}");
        }
        
        if (record.getValue() != null) {
            sql.SET("`value` = #{value,jdbcType=VARCHAR}");
        }
        
        if (record.getCreateTime() != null) {
            sql.SET("create_time = #{createTime,jdbcType=TIMESTAMP}");
        }
        
        if (record.getUseDefaultValue() != null) {
            sql.SET("use_default_value = #{useDefaultValue,jdbcType=TINYINT}");
        }
        
        if (record.getCreateBy() != null) {
            sql.SET("create_by = #{createBy,jdbcType=VARCHAR}");
        }
        
        if (record.getUpdateTime() != null) {
            sql.SET("update_time = #{updateTime,jdbcType=TIMESTAMP}");
        }
        
        if (record.getUpdateBy() != null) {
            sql.SET("update_by = #{updateBy,jdbcType=VARCHAR}");
        }
        
        sql.WHERE("id = #{id,jdbcType=VARCHAR}");
        
        return sql.toString();
    }
}
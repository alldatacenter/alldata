package datart.core.mappers;

import datart.core.entity.Variable;
import org.apache.ibatis.jdbc.SQL;

public class VariableSqlProvider {
    public String insertSelective(Variable record) {
        SQL sql = new SQL();
        sql.INSERT_INTO("variable");
        
        if (record.getId() != null) {
            sql.VALUES("id", "#{id,jdbcType=VARCHAR}");
        }
        
        if (record.getOrgId() != null) {
            sql.VALUES("org_id", "#{orgId,jdbcType=VARCHAR}");
        }
        
        if (record.getViewId() != null) {
            sql.VALUES("view_id", "#{viewId,jdbcType=VARCHAR}");
        }
        
        if (record.getSourceId() != null) {
            sql.VALUES("source_id", "#{sourceId,jdbcType=VARCHAR}");
        }
        
        if (record.getName() != null) {
            sql.VALUES("`name`", "#{name,jdbcType=VARCHAR}");
        }
        
        if (record.getType() != null) {
            sql.VALUES("`type`", "#{type,jdbcType=VARCHAR}");
        }
        
        if (record.getValueType() != null) {
            sql.VALUES("value_type", "#{valueType,jdbcType=VARCHAR}");
        }
        
        if (record.getFormat() != null) {
            sql.VALUES("format", "#{format,jdbcType=VARCHAR}");
        }
        
        if (record.getPermission() != null) {
            sql.VALUES("permission", "#{permission,jdbcType=INTEGER}");
        }
        
        if (record.getEncrypt() != null) {
            sql.VALUES("encrypt", "#{encrypt,jdbcType=TINYINT}");
        }
        
        if (record.getLabel() != null) {
            sql.VALUES("`label`", "#{label,jdbcType=VARCHAR}");
        }
        
        if (record.getDefaultValue() != null) {
            sql.VALUES("default_value", "#{defaultValue,jdbcType=VARCHAR}");
        }
        
        if (record.getExpression() != null) {
            sql.VALUES("expression", "#{expression,jdbcType=TINYINT}");
        }
        
        if (record.getCreateTime() != null) {
            sql.VALUES("create_time", "#{createTime,jdbcType=TIMESTAMP}");
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

    public String updateByPrimaryKeySelective(Variable record) {
        SQL sql = new SQL();
        sql.UPDATE("variable");
        
        if (record.getOrgId() != null) {
            sql.SET("org_id = #{orgId,jdbcType=VARCHAR}");
        }
        
        if (record.getViewId() != null) {
            sql.SET("view_id = #{viewId,jdbcType=VARCHAR}");
        }
        
        if (record.getSourceId() != null) {
            sql.SET("source_id = #{sourceId,jdbcType=VARCHAR}");
        }
        
        if (record.getName() != null) {
            sql.SET("`name` = #{name,jdbcType=VARCHAR}");
        }
        
        if (record.getType() != null) {
            sql.SET("`type` = #{type,jdbcType=VARCHAR}");
        }
        
        if (record.getValueType() != null) {
            sql.SET("value_type = #{valueType,jdbcType=VARCHAR}");
        }
        
        if (record.getFormat() != null) {
            sql.SET("format = #{format,jdbcType=VARCHAR}");
        }
        
        if (record.getPermission() != null) {
            sql.SET("permission = #{permission,jdbcType=INTEGER}");
        }
        
        if (record.getEncrypt() != null) {
            sql.SET("encrypt = #{encrypt,jdbcType=TINYINT}");
        }
        
        if (record.getLabel() != null) {
            sql.SET("`label` = #{label,jdbcType=VARCHAR}");
        }
        
        if (record.getDefaultValue() != null) {
            sql.SET("default_value = #{defaultValue,jdbcType=VARCHAR}");
        }
        
        if (record.getExpression() != null) {
            sql.SET("expression = #{expression,jdbcType=TINYINT}");
        }
        
        if (record.getCreateTime() != null) {
            sql.SET("create_time = #{createTime,jdbcType=TIMESTAMP}");
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
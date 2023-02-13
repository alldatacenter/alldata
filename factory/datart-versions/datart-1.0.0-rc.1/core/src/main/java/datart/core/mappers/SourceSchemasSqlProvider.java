package datart.core.mappers;

import datart.core.entity.SourceSchemas;
import org.apache.ibatis.jdbc.SQL;

public class SourceSchemasSqlProvider {
    public String insertSelective(SourceSchemas record) {
        SQL sql = new SQL();
        sql.INSERT_INTO("source_schemas");
        
        if (record.getId() != null) {
            sql.VALUES("id", "#{id,jdbcType=VARCHAR}");
        }
        
        if (record.getSourceId() != null) {
            sql.VALUES("source_id", "#{sourceId,jdbcType=VARCHAR}");
        }
        
        if (record.getSchemas() != null) {
            sql.VALUES("`schemas`", "#{schemas,jdbcType=VARCHAR}");
        }
        
        if (record.getUpdateTime() != null) {
            sql.VALUES("update_time", "#{updateTime,jdbcType=TIMESTAMP}");
        }
        
        return sql.toString();
    }

    public String updateByPrimaryKeySelective(SourceSchemas record) {
        SQL sql = new SQL();
        sql.UPDATE("source_schemas");
        
        if (record.getSourceId() != null) {
            sql.SET("source_id = #{sourceId,jdbcType=VARCHAR}");
        }
        
        if (record.getSchemas() != null) {
            sql.SET("`schemas` = #{schemas,jdbcType=VARCHAR}");
        }
        
        if (record.getUpdateTime() != null) {
            sql.SET("update_time = #{updateTime,jdbcType=TIMESTAMP}");
        }
        
        sql.WHERE("id = #{id,jdbcType=VARCHAR}");
        
        return sql.toString();
    }
}
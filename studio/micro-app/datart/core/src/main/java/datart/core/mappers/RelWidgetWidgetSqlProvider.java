package datart.core.mappers;

import datart.core.entity.RelWidgetWidget;
import org.apache.ibatis.jdbc.SQL;

public class RelWidgetWidgetSqlProvider {
    public String insertSelective(RelWidgetWidget record) {
        SQL sql = new SQL();
        sql.INSERT_INTO("rel_widget_widget");
        
        if (record.getId() != null) {
            sql.VALUES("id", "#{id,jdbcType=VARCHAR}");
        }
        
        if (record.getSourceId() != null) {
            sql.VALUES("source_id", "#{sourceId,jdbcType=VARCHAR}");
        }
        
        if (record.getTargetId() != null) {
            sql.VALUES("target_id", "#{targetId,jdbcType=VARCHAR}");
        }
        
        if (record.getConfig() != null) {
            sql.VALUES("config", "#{config,jdbcType=VARCHAR}");
        }
        
        return sql.toString();
    }

    public String updateByPrimaryKeySelective(RelWidgetWidget record) {
        SQL sql = new SQL();
        sql.UPDATE("rel_widget_widget");
        
        if (record.getSourceId() != null) {
            sql.SET("source_id = #{sourceId,jdbcType=VARCHAR}");
        }
        
        if (record.getTargetId() != null) {
            sql.SET("target_id = #{targetId,jdbcType=VARCHAR}");
        }
        
        if (record.getConfig() != null) {
            sql.SET("config = #{config,jdbcType=VARCHAR}");
        }
        
        sql.WHERE("id = #{id,jdbcType=VARCHAR}");
        
        return sql.toString();
    }
}
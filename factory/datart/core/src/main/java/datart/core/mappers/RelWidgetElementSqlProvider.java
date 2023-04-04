package datart.core.mappers;

import datart.core.entity.RelWidgetElement;
import org.apache.ibatis.jdbc.SQL;

public class RelWidgetElementSqlProvider {
    public String insertSelective(RelWidgetElement record) {
        SQL sql = new SQL();
        sql.INSERT_INTO("rel_widget_element");
        
        if (record.getId() != null) {
            sql.VALUES("id", "#{id,jdbcType=VARCHAR}");
        }
        
        if (record.getWidgetId() != null) {
            sql.VALUES("widget_id", "#{widgetId,jdbcType=VARCHAR}");
        }
        
        if (record.getRelType() != null) {
            sql.VALUES("rel_type", "#{relType,jdbcType=VARCHAR}");
        }
        
        if (record.getRelId() != null) {
            sql.VALUES("rel_id", "#{relId,jdbcType=VARCHAR}");
        }
        
        return sql.toString();
    }

    public String updateByPrimaryKeySelective(RelWidgetElement record) {
        SQL sql = new SQL();
        sql.UPDATE("rel_widget_element");
        
        if (record.getWidgetId() != null) {
            sql.SET("widget_id = #{widgetId,jdbcType=VARCHAR}");
        }
        
        if (record.getRelType() != null) {
            sql.SET("rel_type = #{relType,jdbcType=VARCHAR}");
        }
        
        if (record.getRelId() != null) {
            sql.SET("rel_id = #{relId,jdbcType=VARCHAR}");
        }
        
        sql.WHERE("id = #{id,jdbcType=VARCHAR}");
        
        return sql.toString();
    }
}
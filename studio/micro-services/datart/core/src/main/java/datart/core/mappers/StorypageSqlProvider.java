package datart.core.mappers;

import datart.core.entity.Storypage;
import org.apache.ibatis.jdbc.SQL;

public class StorypageSqlProvider {
    public String insertSelective(Storypage record) {
        SQL sql = new SQL();
        sql.INSERT_INTO("storypage");
        
        if (record.getId() != null) {
            sql.VALUES("id", "#{id,jdbcType=VARCHAR}");
        }
        
        if (record.getStoryboardId() != null) {
            sql.VALUES("storyboard_id", "#{storyboardId,jdbcType=VARCHAR}");
        }
        
        if (record.getRelType() != null) {
            sql.VALUES("rel_type", "#{relType,jdbcType=VARCHAR}");
        }
        
        if (record.getRelId() != null) {
            sql.VALUES("rel_id", "#{relId,jdbcType=VARCHAR}");
        }
        
        if (record.getConfig() != null) {
            sql.VALUES("config", "#{config,jdbcType=VARCHAR}");
        }
        
        return sql.toString();
    }

    public String updateByPrimaryKeySelective(Storypage record) {
        SQL sql = new SQL();
        sql.UPDATE("storypage");
        
        if (record.getStoryboardId() != null) {
            sql.SET("storyboard_id = #{storyboardId,jdbcType=VARCHAR}");
        }
        
        if (record.getRelType() != null) {
            sql.SET("rel_type = #{relType,jdbcType=VARCHAR}");
        }
        
        if (record.getRelId() != null) {
            sql.SET("rel_id = #{relId,jdbcType=VARCHAR}");
        }
        
        if (record.getConfig() != null) {
            sql.SET("config = #{config,jdbcType=VARCHAR}");
        }
        
        sql.WHERE("id = #{id,jdbcType=VARCHAR}");
        
        return sql.toString();
    }
}
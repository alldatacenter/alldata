package datart.core.mappers;

import datart.core.entity.UserSettings;
import org.apache.ibatis.jdbc.SQL;

public class UserSettingsSqlProvider {
    public String insertSelective(UserSettings record) {
        SQL sql = new SQL();
        sql.INSERT_INTO("user_settings");
        
        if (record.getId() != null) {
            sql.VALUES("id", "#{id,jdbcType=VARCHAR}");
        }
        
        if (record.getUserId() != null) {
            sql.VALUES("user_id", "#{userId,jdbcType=VARCHAR}");
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

    public String updateByPrimaryKeySelective(UserSettings record) {
        SQL sql = new SQL();
        sql.UPDATE("user_settings");
        
        if (record.getUserId() != null) {
            sql.SET("user_id = #{userId,jdbcType=VARCHAR}");
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
package datart.core.mappers;

import datart.core.entity.Folder;
import org.apache.ibatis.jdbc.SQL;

public class FolderSqlProvider {
    public String insertSelective(Folder record) {
        SQL sql = new SQL();
        sql.INSERT_INTO("folder");
        
        if (record.getId() != null) {
            sql.VALUES("id", "#{id,jdbcType=VARCHAR}");
        }
        
        if (record.getName() != null) {
            sql.VALUES("`name`", "#{name,jdbcType=VARCHAR}");
        }
        
        if (record.getOrgId() != null) {
            sql.VALUES("org_id", "#{orgId,jdbcType=VARCHAR}");
        }
        
        if (record.getRelType() != null) {
            sql.VALUES("rel_type", "#{relType,jdbcType=VARCHAR}");
        }
        
        if (record.getSubType() != null) {
            sql.VALUES("sub_type", "#{subType,jdbcType=VARCHAR}");
        }
        
        if (record.getRelId() != null) {
            sql.VALUES("rel_id", "#{relId,jdbcType=VARCHAR}");
        }
        
        if (record.getAvatar() != null) {
            sql.VALUES("avatar", "#{avatar,jdbcType=VARCHAR}");
        }
        
        if (record.getParentId() != null) {
            sql.VALUES("parent_id", "#{parentId,jdbcType=VARCHAR}");
        }
        
        if (record.getIndex() != null) {
            sql.VALUES("`index`", "#{index,jdbcType=DOUBLE}");
        }
        
        return sql.toString();
    }

    public String updateByPrimaryKeySelective(Folder record) {
        SQL sql = new SQL();
        sql.UPDATE("folder");
        
        if (record.getName() != null) {
            sql.SET("`name` = #{name,jdbcType=VARCHAR}");
        }
        
        if (record.getOrgId() != null) {
            sql.SET("org_id = #{orgId,jdbcType=VARCHAR}");
        }
        
        if (record.getRelType() != null) {
            sql.SET("rel_type = #{relType,jdbcType=VARCHAR}");
        }
        
        if (record.getSubType() != null) {
            sql.SET("sub_type = #{subType,jdbcType=VARCHAR}");
        }
        
        if (record.getRelId() != null) {
            sql.SET("rel_id = #{relId,jdbcType=VARCHAR}");
        }
        
        if (record.getAvatar() != null) {
            sql.SET("avatar = #{avatar,jdbcType=VARCHAR}");
        }
        
        if (record.getParentId() != null) {
            sql.SET("parent_id = #{parentId,jdbcType=VARCHAR}");
        }
        
        if (record.getIndex() != null) {
            sql.SET("`index` = #{index,jdbcType=DOUBLE}");
        }
        
        sql.WHERE("id = #{id,jdbcType=VARCHAR}");
        
        return sql.toString();
    }
}
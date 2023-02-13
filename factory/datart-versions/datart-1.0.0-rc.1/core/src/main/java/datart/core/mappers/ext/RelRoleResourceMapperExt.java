package datart.core.mappers.ext;

import datart.core.entity.RelRoleResource;
import datart.core.mappers.RelRoleResourceMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Set;

@CacheNamespaceRef(value = RoleMapperExt.class)
@Mapper
public interface RelRoleResourceMapperExt extends RelRoleResourceMapper {

    @Select({
            "SELECT ",
            "	rrr.*  ",
            "FROM ",
            "	rel_role_resource rrr ",
            "	JOIN rel_role_user rru ",
            "	JOIN `user` u ON u.id = rru.user_id  ",
            "	AND rrr.role_id = rru.role_id  ",
            "	AND u.id = #{userId}"
    })
    List<RelRoleResource> listByUserId(@Param("userId") String userId);

    @Select({
            "SELECT ",
            "	rrr.*  ",
            "FROM ",
            "	rel_role_resource rrr ",
            "	JOIN rel_role_user rru ",
            "	JOIN `user` u ON u.id = rru.user_id  ",
            "	AND rrr.org_id = #{orgId}  ",
            "	AND rrr.role_id = rru.role_id  ",
            "	AND u.id = #{userId} ",
    })
    List<RelRoleResource> listByOrgAndUser(String orgId, String userId);

    @Select({
            "<script>",
            "SELECT ",
            "	rrr.*  ",
            "FROM ",
            "	rel_role_resource rrr  ",
            "WHERE ",
            "	rrr.role_id  IN ",
            "<foreach collection='roleIds' item='item' index='index' open='(' close=')' separator=','>#{item}</foreach>",
            "</script>"
    })
    List<RelRoleResource> selectByRoleIds(Set<String> roleIds);


    @Delete({
            "DELETE ",
            "FROM ",
            "	rel_role_resource ",
            "WHERE ",
            "	role_id = #{roleId} ",
            " AND resource_type=#{resourceType}"
    })
    int deleteByRoleAndResourceType(String roleId, String resourceType);

    @Select({
            "<script>",
            "SELECT COUNT(*) FROM rel_role_resource rrr WHERE ",
            " role_id=#{roleId} ",
            "<if test=\"resourceId==null\">",
            " AND resource_id IS NULL ",
            "</if>",
            "<if test=\"resourceId!=null\">",
            " AND resource_id=#{resourceId} ",
            "</if>",
            "</script>"
    })
    int countRolePermission(String resourceId, String roleId);

    @Select({
            "<script>",
            "SELECT ",
            " *  ",
            "FROM ",
            " rel_role_resource rrr ",
            "WHERE ",
            " rrr.org_id = #{orgId} AND rrr.resource_type=#{resourceType} ",
            "<if test=\"resourceId==null\">",
            " AND resource_id IS NULL ",
            "</if>",
            "<if test=\"resourceId!=null\">",
            " AND resource_id=#{resourceId} ",
            "</if>",
            "</script>"
    })
    List<RelRoleResource> selectByResource(String orgId, String resourceType, String resourceId);


    @Update({
            "<script>",
            "<foreach collection='elements' item='record' index='index' separator=';'>",
            "update rel_role_resource",
            "set role_id = #{roleId,jdbcType=VARCHAR},",
            "resource_id = #{resourceId,jdbcType=VARCHAR},",
            "resource_type = #{resourceType,jdbcType=VARCHAR},",
            "org_id = #{orgId,jdbcType=VARCHAR},",
            "permission = #{permission,jdbcType=INTEGER},",
            "create_by = #{createBy,jdbcType=VARCHAR},",
            "create_time = #{createTime,jdbcType=TIMESTAMP},",
            "update_by = #{updateBy,jdbcType=VARCHAR},",
            "update_time = #{updateTime,jdbcType=TIMESTAMP}",
            "where id = #{id,jdbcType=VARCHAR}",
            "</foreach>",
            "</script>",
    })
    int batchUpdate(List<RelRoleResource> elements);

    @Insert({
            "<script>",
            "insert into rel_role_resource (id, role_id, ",
            "resource_id, resource_type, ",
            "org_id, permission, ",
            "create_by, create_time, ",
            "update_by, update_time) VALUES",
            "<foreach collection='elements' item='record' index='index' separator=','>",
            " <trim prefix='(' suffix=')' suffixOverrides=','>",
            "#{record.id,jdbcType=VARCHAR}, #{record.roleId,jdbcType=VARCHAR}, ",
            "#{record.resourceId,jdbcType=VARCHAR}, #{record.resourceType,jdbcType=VARCHAR}, ",
            "#{record.orgId,jdbcType=VARCHAR}, #{record.permission,jdbcType=INTEGER}, ",
            "#{record.createBy,jdbcType=VARCHAR}, #{record.createTime,jdbcType=TIMESTAMP}, ",
            "#{record.updateBy,jdbcType=VARCHAR}, #{record.updateTime,jdbcType=TIMESTAMP}",
            "	</trim>" +
                    "</foreach>",
            "</script>",
    })
    int batchInsert(List<RelRoleResource> elements);

    @Delete({
            "<script>",
            "DELETE FROM rel_role_resource where resource_type=#{resourceType} ",
            "<if test=\"resourceId==null\">",
            " AND resource_id IS NULL ",
            "</if>",
            "<if test=\"resourceId!=null\">",
            " AND resource_id=#{resourceId} ",
            "</if>",
            "</script>",
    })
    int deleteByResource(String resourceType, String resourceId);

    @Delete({
            "DELETE FROM rel_role_resource where resource_id=#{resourceId}",
    })
    int deleteByResourceId(String resourceId);
}

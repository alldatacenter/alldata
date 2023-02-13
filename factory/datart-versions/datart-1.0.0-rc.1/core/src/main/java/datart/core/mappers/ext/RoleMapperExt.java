package datart.core.mappers.ext;

import datart.core.entity.Role;
import datart.core.entity.User;
import datart.core.mappers.RoleMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

@CacheNamespace(flushInterval = 5 * 1000)
@Mapper
public interface RoleMapperExt extends RoleMapper {

    @Select({
            "SELECT " +
                    "	r.*  " +
                    "FROM " +
                    "	role r " +
                    "	JOIN rel_role_user rru ON rru.role_id = r.id  " +
                    "	AND rru.user_id =#{userId}"
    })
    List<Role> selectUserAllRoles(@Param("userId") String userId);

    @Select({
            "SELECT ",
            "	r.*  ",
            "FROM ",
            "	role r ",
            "	JOIN rel_role_user rru ON rru.role_id = r.id ",
            "	AND rru.user_id =#{userId} ",
            "   AND r.org_id = #{orgId}"
    })
    List<Role> selectByOrgAndUser(String orgId, String userId);

    @Select({
            "SELECT " +
                    "	r.*  " +
                    "FROM " +
                    "	role r " +
                    "	JOIN rel_role_user rru ON rru.role_id = r.id  " +
                    "	AND rru.user_id =#{userId} AND r.org_id=#{orgId} AND r.type NOT IN ('ORG_OWNER','PER_USER')"
    })
    List<Role> listUserGeneralRoles(String orgId, String userId);


    @Select({
            "SELECT * FROM role r WHERE r.type='PER_USER' AND r.org_id=#{orgId} AND r.create_by=#{userId}"
    })
    Role selectPerUserRole(String orgId, String userId);

    @Select({
            "SELECT " +
                    " *  " +
                    "FROM " +
                    " `user` u " +
                    " JOIN rel_role_user rru " +
                    " JOIN role r ON u.id = rru.user_id  " +
                    " AND r.id = #{roleId}  " +
                    " AND r.type = 'PER_USER'  " +
                    " AND r.id = rru.role_id"
    })
    User selectPerUserRoleUser(String roleId);

    @Select({
            "SELECT " +
                    "	r.*  " +
                    "FROM " +
                    "	`role` r  " +
                    "WHERE " +
                    "	r.org_id = #{orgId} and r.type='NORMAL' ORDER BY create_time ASC"
    })
    List<Role> listByOrgId(@Param("orgId") String orgId);


    @Select({
            "SELECT " +
                    "	u.*  " +
                    "FROM " +
                    "	`user` u " +
                    "	JOIN rel_role_user rru ON rru.user_id = u.id  " +
                    "	AND rru.role_id = #{roleId}"
    })
    List<User> listRoleUsers(@Param("roleId") String roleId);

    @Select({
            "<script>",
            "SELECT * FROM `role` WHERE `id` IN ",
            "<foreach collection='roleIds' item='item' index='index' open='(' close=')' separator=','>  #{item} </foreach> ;",
            "</script>"
    })
    List<Role> listByIds(List<String> roleIds);

    @Delete({
            "DELETE FROM role WHERE id = #{roleId};",
            "DELETE FROM rel_role_user WHERE role_id = #{roleId};",
            "DELETE FROM rel_subject_columns WHERE subject_id = #{roleId};",
            "DELETE FROM rel_role_resource WHERE role_id = #{roleId};"
    })
    int deleteRole(@Param("roleId") String roleId);


    @Select({
            "SELECT " +
                    "	r.* " +
                    "FROM " +
                    "	role r " +
                    "WHERE " +
                    "	r.type = 'ORG_OWNER' " +
                    "AND r.org_id = #{orgId}"
    })
    Role selectOrgOwnerRole(@Param("orgId") String orgId);

    @Select({
            "SELECT u.* FROM `user` u JOIN rel_role_user rru JOIN role r ON u.id=rru.user_id AND rru.role_id=r.id AND r.type='ORG_OWNER' AND r.org_id=#{orgId}"
    })
    List<User> selectOrgOwners(String orgId);


    @Select({
            "SELECT ",
            "	r.*  ",
            "FROM ",
            "	role r ",
            "	JOIN rel_role_user rru ON r.id IN ( ",
            "	SELECT DISTINCT ",
            "		r1.id  ",
            "	FROM ",
            "		role r1  ",
            "	WHERE ",
            "		r1.org_id = #{orgId}) ",
            "		 ",
            "	AND r.id = rru.role_id  ",
            "	AND rru.user_id = #{userId}"
    })
    List<Role> selectUserRoles(@Param("orgId") String orgId, @Param("userId") String userId);


}
package datart.core.mappers.ext;

import datart.core.entity.Organization;
import datart.core.entity.User;
import datart.core.mappers.OrganizationMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;


@Mapper
public interface OrganizationMapperExt extends OrganizationMapper {

    @Select({"SELECT * FROM organization "})
    List<Organization> list();

    @Select({
            "SELECT " +
                    "org.* " +
                    "FROM " +
                    "organization org " +
                    "JOIN rel_user_organization rel ON (org.id=rel.org_id AND rel.user_id=#{userId})"
    })
    List<Organization> listOrganizationsByUserId(@Param("userId") String userId);

    @Select({
            "SELECT " +
                    "	u.*  " +
                    "FROM " +
                    "	`user` u " +
                    "	JOIN rel_user_organization ruo ON u.id = ruo.user_id  " +
                    "	AND ruo.org_id = #{orgId}"
    })
    List<User> listOrgMembers(@Param("orgId") String orgId);

    @Delete({
            "DELETE FROM rel_user_organization WHERE user_id=#{userId} AND org_id=#{orgId};",
            "DELETE FROM rel_role_user WHERE user_id=#{userId} AND role_id IN (SELECT DISTINCT id FROM role r WHERE r.org_id=#{orgId});",
            "DELETE FROM role WHERE `type`='PER_USER' AND org_id=#{orgId} AND create_by=#{userId};",
    })
    int deleteOrgMember(@Param("orgId") String orgId, @Param("userId") String userId);


    @Delete(value = {
            "DELETE FROM `dashboard` WHERE org_id = #{orgId};",
            "DELETE FROM `datachart` WHERE org_id = #{orgId};",
            "DELETE FROM `source` WHERE org_id = #{orgId};",
            "DELETE FROM `view` WHERE org_id = #{orgId};",
            "DELETE FROM `schedule` WHERE org_id = #{orgId};",
            "DELETE FROM `folder` WHERE org_id = #{orgId};",
            "DELETE FROM rel_role_user WHERE role_id IN (SELECT DISTINCT r.id FROM role r WHERE r.org_id=#{orgId});",
            "DELETE FROM rel_role_resource  WHERE role_id IN (SELECT DISTINCT r.id FROM role r WHERE r.org_id=#{orgId});",
            "DELETE FROM role WHERE org_id = #{orgId};",
            "DELETE FROM organization WHERE id = #{orgId};"
    })
    void deleteOrg(@Param("orgId") String orgId);

    @Select("SELECT o.* FROM organization o WHERE id = #{orgId} FOR UPDATE")
    Organization selectForUpdate(@Param("orgId") String orgId);

    @Select({
            "SELECT o.* FROM organization o WHERE o.name=#{name}"
    })
    Organization selectOrgByName(@Param("name") String name);


    @Select({
            "SELECT ",
            " *  ",
            "FROM ",
            " `user` u ",
            " JOIN rel_role_user rru ",
            " JOIN role r ON r.type = 'ORG_OWNER'  ",
            " AND r.org_id = #{orgId} AND r.id = rru.role_id AND rru.user_id = u.id"
    })
    List<User> selectOrgOwners(String orgId);

}
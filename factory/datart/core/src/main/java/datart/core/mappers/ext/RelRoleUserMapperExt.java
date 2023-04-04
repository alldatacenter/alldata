package datart.core.mappers.ext;

import datart.core.entity.RelRoleUser;
import datart.core.entity.Role;
import datart.core.mappers.RelRoleUserMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

@CacheNamespaceRef(value = RoleMapperExt.class)
@Mapper
public interface RelRoleUserMapperExt extends RelRoleUserMapper {

    @Insert({
            "<script>",
            "	insert ignore into rel_role_user" +
                    "		<trim prefix='(' suffix=')' suffixOverrides=','>" +
                    "			`id`," +
                    "			`user_id`," +
                    "			`role_id`," +
                    "			`create_by`," +
                    "			`create_time`" +
                    "		</trim>" +
                    "		VALUES" +
                    "		<foreach collection='relRoleUsers' item='record' index='index' separator=','>" +
                    "			<trim prefix='(' suffix=')' suffixOverrides=','>" +
                    "				#{record.id,jdbcType=VARCHAR}," +
                    "				#{record.userId,jdbcType=VARCHAR}," +
                    "				#{record.roleId,jdbcType=VARCHAR}," +
                    "				#{record.createBy,jdbcType=VARCHAR}," +
                    "				#{record.createTime,jdbcType=TIMESTAMP}" +
                    "			</trim>" +
                    "		</foreach>",
            "</script>"
    })
    int insertBatch(@Param("relRoleUsers") List<RelRoleUser> relRoleUsers);


    @Delete({
            "<script>",
            "	delete from rel_role_user where role_id = #{roleId}" +
                    "		<if test='userIds != null and userIds.size > 0'>" +
                    "			and user_id in" +
                    "			<foreach collection='userIds' item='item' index='index' open='(' close=')' separator=','>" +
                    "				#{item}" +
                    "			</foreach>" +
                    "		</if>" +
                    "		<if test='userIds == null or userIds.size == 0'>" +
                    "			and 1=0" +
                    "		</if>",
            "</script>"
    })
    int deleteByRoleIdAndUserIds(@Param("roleId") String roleId, @Param("userIds") List<String> userIds);

    @Select({
            "SELECT * FROM rel_role_user rru WHERE rru.user_id = #{userId}"
    })
    List<Role> listByUserId(String userId);

    @Select({
            "SELECT * FROM rel_role_user WHERE role_id= #{roleId} AND user_id=#{userId}"
    })
    RelRoleUser selectByUserAndRole(String userId, String roleId);

    @Delete({
            "DELETE FROM rel_role_user WHERE role_id= #{roleId} AND user_id=#{userId}"
    })
    int deleteByUserAndRole(String userId, String roleId);

    @Delete({
            "<script>",
            "DELETE FROM rel_role_user WHERE user_id=#{userId}",
            "		<if test='roleIds != null and roleIds.size > 0'>" +
                    "			and role_id in" +
                    "			<foreach collection='roleIds' item='item' index='index' open='(' close=')' separator=','>" +
                    "				#{item}" +
                    "			</foreach>" +
                    "		</if>" +
                    "		<if test='roleIds == null or roleIds.size == 0'>" +
                    "			and 1=0" +
                    "		</if>",
            "</script>"
    })
    int deleteByUserAndRoles(String userId, List<String> roleIds);


}

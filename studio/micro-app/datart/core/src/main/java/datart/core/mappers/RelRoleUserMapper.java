package datart.core.mappers;

import datart.core.entity.RelRoleUser;
import datart.core.mappers.ext.CRUDMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.UpdateProvider;
import org.apache.ibatis.type.JdbcType;

public interface RelRoleUserMapper extends CRUDMapper {
    @Delete({
        "delete from rel_role_user",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int deleteByPrimaryKey(String id);

    @Insert({
        "insert into rel_role_user (id, user_id, ",
        "role_id, create_by, ",
        "create_time, update_by, ",
        "update_time)",
        "values (#{id,jdbcType=VARCHAR}, #{userId,jdbcType=VARCHAR}, ",
        "#{roleId,jdbcType=VARCHAR}, #{createBy,jdbcType=VARCHAR}, ",
        "#{createTime,jdbcType=TIMESTAMP}, #{updateBy,jdbcType=VARCHAR}, ",
        "#{updateTime,jdbcType=TIMESTAMP})"
    })
    int insert(RelRoleUser record);

    @InsertProvider(type=RelRoleUserSqlProvider.class, method="insertSelective")
    int insertSelective(RelRoleUser record);

    @Select({
        "select",
        "id, user_id, role_id, create_by, create_time, update_by, update_time",
        "from rel_role_user",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    @Results({
        @Result(column="id", property="id", jdbcType=JdbcType.VARCHAR, id=true),
        @Result(column="user_id", property="userId", jdbcType=JdbcType.VARCHAR),
        @Result(column="role_id", property="roleId", jdbcType=JdbcType.VARCHAR),
        @Result(column="create_by", property="createBy", jdbcType=JdbcType.VARCHAR),
        @Result(column="create_time", property="createTime", jdbcType=JdbcType.TIMESTAMP),
        @Result(column="update_by", property="updateBy", jdbcType=JdbcType.VARCHAR),
        @Result(column="update_time", property="updateTime", jdbcType=JdbcType.TIMESTAMP)
    })
    RelRoleUser selectByPrimaryKey(String id);

    @UpdateProvider(type=RelRoleUserSqlProvider.class, method="updateByPrimaryKeySelective")
    int updateByPrimaryKeySelective(RelRoleUser record);

    @Update({
        "update rel_role_user",
        "set user_id = #{userId,jdbcType=VARCHAR},",
          "role_id = #{roleId,jdbcType=VARCHAR},",
          "create_by = #{createBy,jdbcType=VARCHAR},",
          "create_time = #{createTime,jdbcType=TIMESTAMP},",
          "update_by = #{updateBy,jdbcType=VARCHAR},",
          "update_time = #{updateTime,jdbcType=TIMESTAMP}",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int updateByPrimaryKey(RelRoleUser record);
}
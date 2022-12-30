package datart.core.mappers;

import datart.core.entity.RelUserOrganization;
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

public interface RelUserOrganizationMapper extends CRUDMapper {
    @Delete({
        "delete from rel_user_organization",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int deleteByPrimaryKey(String id);

    @Insert({
        "insert into rel_user_organization (id, org_id, ",
        "user_id, create_by, ",
        "create_time, update_by, ",
        "update_time)",
        "values (#{id,jdbcType=VARCHAR}, #{orgId,jdbcType=VARCHAR}, ",
        "#{userId,jdbcType=VARCHAR}, #{createBy,jdbcType=VARCHAR}, ",
        "#{createTime,jdbcType=TIMESTAMP}, #{updateBy,jdbcType=VARCHAR}, ",
        "#{updateTime,jdbcType=TIMESTAMP})"
    })
    int insert(RelUserOrganization record);

    @InsertProvider(type=RelUserOrganizationSqlProvider.class, method="insertSelective")
    int insertSelective(RelUserOrganization record);

    @Select({
        "select",
        "id, org_id, user_id, create_by, create_time, update_by, update_time",
        "from rel_user_organization",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    @Results({
        @Result(column="id", property="id", jdbcType=JdbcType.VARCHAR, id=true),
        @Result(column="org_id", property="orgId", jdbcType=JdbcType.VARCHAR),
        @Result(column="user_id", property="userId", jdbcType=JdbcType.VARCHAR),
        @Result(column="create_by", property="createBy", jdbcType=JdbcType.VARCHAR),
        @Result(column="create_time", property="createTime", jdbcType=JdbcType.TIMESTAMP),
        @Result(column="update_by", property="updateBy", jdbcType=JdbcType.VARCHAR),
        @Result(column="update_time", property="updateTime", jdbcType=JdbcType.TIMESTAMP)
    })
    RelUserOrganization selectByPrimaryKey(String id);

    @UpdateProvider(type=RelUserOrganizationSqlProvider.class, method="updateByPrimaryKeySelective")
    int updateByPrimaryKeySelective(RelUserOrganization record);

    @Update({
        "update rel_user_organization",
        "set org_id = #{orgId,jdbcType=VARCHAR},",
          "user_id = #{userId,jdbcType=VARCHAR},",
          "create_by = #{createBy,jdbcType=VARCHAR},",
          "create_time = #{createTime,jdbcType=TIMESTAMP},",
          "update_by = #{updateBy,jdbcType=VARCHAR},",
          "update_time = #{updateTime,jdbcType=TIMESTAMP}",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int updateByPrimaryKey(RelUserOrganization record);
}
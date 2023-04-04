package datart.core.mappers;

import datart.core.entity.OrgSettings;
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

public interface OrgSettingsMapper extends CRUDMapper {
    @Delete({
        "delete from org_settings",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int deleteByPrimaryKey(String id);

    @Insert({
        "insert into org_settings (id, org_id, ",
        "`type`, config)",
        "values (#{id,jdbcType=VARCHAR}, #{orgId,jdbcType=VARCHAR}, ",
        "#{type,jdbcType=VARCHAR}, #{config,jdbcType=VARCHAR})"
    })
    int insert(OrgSettings record);

    @InsertProvider(type=OrgSettingsSqlProvider.class, method="insertSelective")
    int insertSelective(OrgSettings record);

    @Select({
        "select",
        "id, org_id, `type`, config",
        "from org_settings",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    @Results({
        @Result(column="id", property="id", jdbcType=JdbcType.VARCHAR, id=true),
        @Result(column="org_id", property="orgId", jdbcType=JdbcType.VARCHAR),
        @Result(column="type", property="type", jdbcType=JdbcType.VARCHAR),
        @Result(column="config", property="config", jdbcType=JdbcType.VARCHAR)
    })
    OrgSettings selectByPrimaryKey(String id);

    @UpdateProvider(type=OrgSettingsSqlProvider.class, method="updateByPrimaryKeySelective")
    int updateByPrimaryKeySelective(OrgSettings record);

    @Update({
        "update org_settings",
        "set org_id = #{orgId,jdbcType=VARCHAR},",
          "`type` = #{type,jdbcType=VARCHAR},",
          "config = #{config,jdbcType=VARCHAR}",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int updateByPrimaryKey(OrgSettings record);
}
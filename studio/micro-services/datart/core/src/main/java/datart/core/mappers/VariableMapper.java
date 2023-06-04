package datart.core.mappers;

import datart.core.entity.Variable;
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

public interface VariableMapper extends CRUDMapper {
    @Delete({
        "delete from variable",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int deleteByPrimaryKey(String id);

    @Insert({
        "insert into variable (id, org_id, ",
        "view_id, source_id, ",
        "`name`, `type`, value_type, ",
        "format, permission, ",
        "encrypt, `label`, ",
        "default_value, expression, ",
        "create_time, create_by, ",
        "update_time, update_by)",
        "values (#{id,jdbcType=VARCHAR}, #{orgId,jdbcType=VARCHAR}, ",
        "#{viewId,jdbcType=VARCHAR}, #{sourceId,jdbcType=VARCHAR}, ",
        "#{name,jdbcType=VARCHAR}, #{type,jdbcType=VARCHAR}, #{valueType,jdbcType=VARCHAR}, ",
        "#{format,jdbcType=VARCHAR}, #{permission,jdbcType=INTEGER}, ",
        "#{encrypt,jdbcType=TINYINT}, #{label,jdbcType=VARCHAR}, ",
        "#{defaultValue,jdbcType=VARCHAR}, #{expression,jdbcType=TINYINT}, ",
        "#{createTime,jdbcType=TIMESTAMP}, #{createBy,jdbcType=VARCHAR}, ",
        "#{updateTime,jdbcType=TIMESTAMP}, #{updateBy,jdbcType=VARCHAR})"
    })
    int insert(Variable record);

    @InsertProvider(type=VariableSqlProvider.class, method="insertSelective")
    int insertSelective(Variable record);

    @Select({
        "select",
        "id, org_id, view_id, source_id, `name`, `type`, value_type, format, permission, ",
        "encrypt, `label`, default_value, expression, create_time, create_by, update_time, ",
        "update_by",
        "from variable",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    @Results({
        @Result(column="id", property="id", jdbcType=JdbcType.VARCHAR, id=true),
        @Result(column="org_id", property="orgId", jdbcType=JdbcType.VARCHAR),
        @Result(column="view_id", property="viewId", jdbcType=JdbcType.VARCHAR),
        @Result(column="source_id", property="sourceId", jdbcType=JdbcType.VARCHAR),
        @Result(column="name", property="name", jdbcType=JdbcType.VARCHAR),
        @Result(column="type", property="type", jdbcType=JdbcType.VARCHAR),
        @Result(column="value_type", property="valueType", jdbcType=JdbcType.VARCHAR),
        @Result(column="format", property="format", jdbcType=JdbcType.VARCHAR),
        @Result(column="permission", property="permission", jdbcType=JdbcType.INTEGER),
        @Result(column="encrypt", property="encrypt", jdbcType=JdbcType.TINYINT),
        @Result(column="label", property="label", jdbcType=JdbcType.VARCHAR),
        @Result(column="default_value", property="defaultValue", jdbcType=JdbcType.VARCHAR),
        @Result(column="expression", property="expression", jdbcType=JdbcType.TINYINT),
        @Result(column="create_time", property="createTime", jdbcType=JdbcType.TIMESTAMP),
        @Result(column="create_by", property="createBy", jdbcType=JdbcType.VARCHAR),
        @Result(column="update_time", property="updateTime", jdbcType=JdbcType.TIMESTAMP),
        @Result(column="update_by", property="updateBy", jdbcType=JdbcType.VARCHAR)
    })
    Variable selectByPrimaryKey(String id);

    @UpdateProvider(type=VariableSqlProvider.class, method="updateByPrimaryKeySelective")
    int updateByPrimaryKeySelective(Variable record);

    @Update({
        "update variable",
        "set org_id = #{orgId,jdbcType=VARCHAR},",
          "view_id = #{viewId,jdbcType=VARCHAR},",
          "source_id = #{sourceId,jdbcType=VARCHAR},",
          "`name` = #{name,jdbcType=VARCHAR},",
          "`type` = #{type,jdbcType=VARCHAR},",
          "value_type = #{valueType,jdbcType=VARCHAR},",
          "format = #{format,jdbcType=VARCHAR},",
          "permission = #{permission,jdbcType=INTEGER},",
          "encrypt = #{encrypt,jdbcType=TINYINT},",
          "`label` = #{label,jdbcType=VARCHAR},",
          "default_value = #{defaultValue,jdbcType=VARCHAR},",
          "expression = #{expression,jdbcType=TINYINT},",
          "create_time = #{createTime,jdbcType=TIMESTAMP},",
          "create_by = #{createBy,jdbcType=VARCHAR},",
          "update_time = #{updateTime,jdbcType=TIMESTAMP},",
          "update_by = #{updateBy,jdbcType=VARCHAR}",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int updateByPrimaryKey(Variable record);
}
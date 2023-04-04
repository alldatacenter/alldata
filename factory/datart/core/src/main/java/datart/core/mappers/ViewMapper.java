package datart.core.mappers;

import datart.core.entity.View;
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

public interface ViewMapper extends CRUDMapper {
    @Delete({
        "delete from view",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int deleteByPrimaryKey(String id);

    @Insert({
        "insert into view (id, `name`, ",
        "description, org_id, ",
        "source_id, script, `type`, ",
        "model, config, create_by, ",
        "create_time, update_by, ",
        "update_time, parent_id, ",
        "is_folder, `index`, ",
        "`status`)",
        "values (#{id,jdbcType=VARCHAR}, #{name,jdbcType=VARCHAR}, ",
        "#{description,jdbcType=VARCHAR}, #{orgId,jdbcType=VARCHAR}, ",
        "#{sourceId,jdbcType=VARCHAR}, #{script,jdbcType=VARCHAR}, #{type,jdbcType=VARCHAR}, ",
        "#{model,jdbcType=VARCHAR}, #{config,jdbcType=VARCHAR}, #{createBy,jdbcType=VARCHAR}, ",
        "#{createTime,jdbcType=TIMESTAMP}, #{updateBy,jdbcType=VARCHAR}, ",
        "#{updateTime,jdbcType=TIMESTAMP}, #{parentId,jdbcType=VARCHAR}, ",
        "#{isFolder,jdbcType=TINYINT}, #{index,jdbcType=DOUBLE}, ",
        "#{status,jdbcType=TINYINT})"
    })
    int insert(View record);

    @InsertProvider(type=ViewSqlProvider.class, method="insertSelective")
    int insertSelective(View record);

    @Select({
        "select",
        "id, `name`, description, org_id, source_id, script, `type`, model, config, create_by, ",
        "create_time, update_by, update_time, parent_id, is_folder, `index`, `status`",
        "from view",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    @Results({
        @Result(column="id", property="id", jdbcType=JdbcType.VARCHAR, id=true),
        @Result(column="name", property="name", jdbcType=JdbcType.VARCHAR),
        @Result(column="description", property="description", jdbcType=JdbcType.VARCHAR),
        @Result(column="org_id", property="orgId", jdbcType=JdbcType.VARCHAR),
        @Result(column="source_id", property="sourceId", jdbcType=JdbcType.VARCHAR),
        @Result(column="script", property="script", jdbcType=JdbcType.VARCHAR),
        @Result(column="type", property="type", jdbcType=JdbcType.VARCHAR),
        @Result(column="model", property="model", jdbcType=JdbcType.VARCHAR),
        @Result(column="config", property="config", jdbcType=JdbcType.VARCHAR),
        @Result(column="create_by", property="createBy", jdbcType=JdbcType.VARCHAR),
        @Result(column="create_time", property="createTime", jdbcType=JdbcType.TIMESTAMP),
        @Result(column="update_by", property="updateBy", jdbcType=JdbcType.VARCHAR),
        @Result(column="update_time", property="updateTime", jdbcType=JdbcType.TIMESTAMP),
        @Result(column="parent_id", property="parentId", jdbcType=JdbcType.VARCHAR),
        @Result(column="is_folder", property="isFolder", jdbcType=JdbcType.TINYINT),
        @Result(column="index", property="index", jdbcType=JdbcType.DOUBLE),
        @Result(column="status", property="status", jdbcType=JdbcType.TINYINT)
    })
    View selectByPrimaryKey(String id);

    @UpdateProvider(type=ViewSqlProvider.class, method="updateByPrimaryKeySelective")
    int updateByPrimaryKeySelective(View record);

    @Update({
        "update view",
        "set `name` = #{name,jdbcType=VARCHAR},",
          "description = #{description,jdbcType=VARCHAR},",
          "org_id = #{orgId,jdbcType=VARCHAR},",
          "source_id = #{sourceId,jdbcType=VARCHAR},",
          "script = #{script,jdbcType=VARCHAR},",
          "`type` = #{type,jdbcType=VARCHAR},",
          "model = #{model,jdbcType=VARCHAR},",
          "config = #{config,jdbcType=VARCHAR},",
          "create_by = #{createBy,jdbcType=VARCHAR},",
          "create_time = #{createTime,jdbcType=TIMESTAMP},",
          "update_by = #{updateBy,jdbcType=VARCHAR},",
          "update_time = #{updateTime,jdbcType=TIMESTAMP},",
          "parent_id = #{parentId,jdbcType=VARCHAR},",
          "is_folder = #{isFolder,jdbcType=TINYINT},",
          "`index` = #{index,jdbcType=DOUBLE},",
          "`status` = #{status,jdbcType=TINYINT}",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int updateByPrimaryKey(View record);
}
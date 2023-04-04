package datart.core.mappers;

import datart.core.entity.Folder;
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

public interface FolderMapper extends CRUDMapper {
    @Delete({
        "delete from folder",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int deleteByPrimaryKey(String id);

    @Insert({
        "insert into folder (id, `name`, ",
        "org_id, rel_type, ",
        "sub_type, rel_id, ",
        "avatar, parent_id, ",
        "`index`)",
        "values (#{id,jdbcType=VARCHAR}, #{name,jdbcType=VARCHAR}, ",
        "#{orgId,jdbcType=VARCHAR}, #{relType,jdbcType=VARCHAR}, ",
        "#{subType,jdbcType=VARCHAR}, #{relId,jdbcType=VARCHAR}, ",
        "#{avatar,jdbcType=VARCHAR}, #{parentId,jdbcType=VARCHAR}, ",
        "#{index,jdbcType=DOUBLE})"
    })
    int insert(Folder record);

    @InsertProvider(type=FolderSqlProvider.class, method="insertSelective")
    int insertSelective(Folder record);

    @Select({
        "select",
        "id, `name`, org_id, rel_type, sub_type, rel_id, avatar, parent_id, `index`",
        "from folder",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    @Results({
        @Result(column="id", property="id", jdbcType=JdbcType.VARCHAR, id=true),
        @Result(column="name", property="name", jdbcType=JdbcType.VARCHAR),
        @Result(column="org_id", property="orgId", jdbcType=JdbcType.VARCHAR),
        @Result(column="rel_type", property="relType", jdbcType=JdbcType.VARCHAR),
        @Result(column="sub_type", property="subType", jdbcType=JdbcType.VARCHAR),
        @Result(column="rel_id", property="relId", jdbcType=JdbcType.VARCHAR),
        @Result(column="avatar", property="avatar", jdbcType=JdbcType.VARCHAR),
        @Result(column="parent_id", property="parentId", jdbcType=JdbcType.VARCHAR),
        @Result(column="index", property="index", jdbcType=JdbcType.DOUBLE)
    })
    Folder selectByPrimaryKey(String id);

    @UpdateProvider(type=FolderSqlProvider.class, method="updateByPrimaryKeySelective")
    int updateByPrimaryKeySelective(Folder record);

    @Update({
        "update folder",
        "set `name` = #{name,jdbcType=VARCHAR},",
          "org_id = #{orgId,jdbcType=VARCHAR},",
          "rel_type = #{relType,jdbcType=VARCHAR},",
          "sub_type = #{subType,jdbcType=VARCHAR},",
          "rel_id = #{relId,jdbcType=VARCHAR},",
          "avatar = #{avatar,jdbcType=VARCHAR},",
          "parent_id = #{parentId,jdbcType=VARCHAR},",
          "`index` = #{index,jdbcType=DOUBLE}",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int updateByPrimaryKey(Folder record);
}
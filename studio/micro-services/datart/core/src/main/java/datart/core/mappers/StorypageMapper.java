package datart.core.mappers;

import datart.core.entity.Storypage;
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

public interface StorypageMapper extends CRUDMapper {
    @Delete({
        "delete from storypage",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int deleteByPrimaryKey(String id);

    @Insert({
        "insert into storypage (id, storyboard_id, ",
        "rel_type, rel_id, ",
        "config)",
        "values (#{id,jdbcType=VARCHAR}, #{storyboardId,jdbcType=VARCHAR}, ",
        "#{relType,jdbcType=VARCHAR}, #{relId,jdbcType=VARCHAR}, ",
        "#{config,jdbcType=VARCHAR})"
    })
    int insert(Storypage record);

    @InsertProvider(type=StorypageSqlProvider.class, method="insertSelective")
    int insertSelective(Storypage record);

    @Select({
        "select",
        "id, storyboard_id, rel_type, rel_id, config",
        "from storypage",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    @Results({
        @Result(column="id", property="id", jdbcType=JdbcType.VARCHAR, id=true),
        @Result(column="storyboard_id", property="storyboardId", jdbcType=JdbcType.VARCHAR),
        @Result(column="rel_type", property="relType", jdbcType=JdbcType.VARCHAR),
        @Result(column="rel_id", property="relId", jdbcType=JdbcType.VARCHAR),
        @Result(column="config", property="config", jdbcType=JdbcType.VARCHAR)
    })
    Storypage selectByPrimaryKey(String id);

    @UpdateProvider(type=StorypageSqlProvider.class, method="updateByPrimaryKeySelective")
    int updateByPrimaryKeySelective(Storypage record);

    @Update({
        "update storypage",
        "set storyboard_id = #{storyboardId,jdbcType=VARCHAR},",
          "rel_type = #{relType,jdbcType=VARCHAR},",
          "rel_id = #{relId,jdbcType=VARCHAR},",
          "config = #{config,jdbcType=VARCHAR}",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int updateByPrimaryKey(Storypage record);
}
package datart.core.mappers;

import datart.core.entity.RelWidgetWidget;
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

public interface RelWidgetWidgetMapper extends CRUDMapper {
    @Delete({
        "delete from rel_widget_widget",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int deleteByPrimaryKey(String id);

    @Insert({
        "insert into rel_widget_widget (id, source_id, ",
        "target_id, config)",
        "values (#{id,jdbcType=VARCHAR}, #{sourceId,jdbcType=VARCHAR}, ",
        "#{targetId,jdbcType=VARCHAR}, #{config,jdbcType=VARCHAR})"
    })
    int insert(RelWidgetWidget record);

    @InsertProvider(type=RelWidgetWidgetSqlProvider.class, method="insertSelective")
    int insertSelective(RelWidgetWidget record);

    @Select({
        "select",
        "id, source_id, target_id, config",
        "from rel_widget_widget",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    @Results({
        @Result(column="id", property="id", jdbcType=JdbcType.VARCHAR, id=true),
        @Result(column="source_id", property="sourceId", jdbcType=JdbcType.VARCHAR),
        @Result(column="target_id", property="targetId", jdbcType=JdbcType.VARCHAR),
        @Result(column="config", property="config", jdbcType=JdbcType.VARCHAR)
    })
    RelWidgetWidget selectByPrimaryKey(String id);

    @UpdateProvider(type=RelWidgetWidgetSqlProvider.class, method="updateByPrimaryKeySelective")
    int updateByPrimaryKeySelective(RelWidgetWidget record);

    @Update({
        "update rel_widget_widget",
        "set source_id = #{sourceId,jdbcType=VARCHAR},",
          "target_id = #{targetId,jdbcType=VARCHAR},",
          "config = #{config,jdbcType=VARCHAR}",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int updateByPrimaryKey(RelWidgetWidget record);
}
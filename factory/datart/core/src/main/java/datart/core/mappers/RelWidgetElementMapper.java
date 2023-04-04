package datart.core.mappers;

import datart.core.entity.RelWidgetElement;
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

public interface RelWidgetElementMapper extends CRUDMapper {
    @Delete({
        "delete from rel_widget_element",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int deleteByPrimaryKey(String id);

    @Insert({
        "insert into rel_widget_element (id, widget_id, ",
        "rel_type, rel_id)",
        "values (#{id,jdbcType=VARCHAR}, #{widgetId,jdbcType=VARCHAR}, ",
        "#{relType,jdbcType=VARCHAR}, #{relId,jdbcType=VARCHAR})"
    })
    int insert(RelWidgetElement record);

    @InsertProvider(type=RelWidgetElementSqlProvider.class, method="insertSelective")
    int insertSelective(RelWidgetElement record);

    @Select({
        "select",
        "id, widget_id, rel_type, rel_id",
        "from rel_widget_element",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    @Results({
        @Result(column="id", property="id", jdbcType=JdbcType.VARCHAR, id=true),
        @Result(column="widget_id", property="widgetId", jdbcType=JdbcType.VARCHAR),
        @Result(column="rel_type", property="relType", jdbcType=JdbcType.VARCHAR),
        @Result(column="rel_id", property="relId", jdbcType=JdbcType.VARCHAR)
    })
    RelWidgetElement selectByPrimaryKey(String id);

    @UpdateProvider(type=RelWidgetElementSqlProvider.class, method="updateByPrimaryKeySelective")
    int updateByPrimaryKeySelective(RelWidgetElement record);

    @Update({
        "update rel_widget_element",
        "set widget_id = #{widgetId,jdbcType=VARCHAR},",
          "rel_type = #{relType,jdbcType=VARCHAR},",
          "rel_id = #{relId,jdbcType=VARCHAR}",
        "where id = #{id,jdbcType=VARCHAR}"
    })
    int updateByPrimaryKey(RelWidgetElement record);
}
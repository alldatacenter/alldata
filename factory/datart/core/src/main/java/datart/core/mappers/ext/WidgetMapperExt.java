package datart.core.mappers.ext;

import datart.core.entity.Widget;
import datart.core.mappers.WidgetMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface WidgetMapperExt extends WidgetMapper {

    @Select({
            "SELECT w.* " +
                    "FROM widget w " +
                    "WHERE dashboard_id = #{dashboardId}"
    })
    List<Widget> listByDashboard(String dashboardId);

    @Insert({
            "<script>",
            "INSERT INTO widget (id, dashboard_id," +
                    "        config, parent_id, " +
                    "        create_by, create_time," +
                    "        update_by, update_time) VALUES " +
                    "<foreach collection='widgets' item='record' index='index' separator=','>" +
                    "	<trim prefix='(' suffix=')' suffixOverrides=','>" +
                    " #{record.id,jdbcType=VARCHAR}," +
                    " #{record.dashboardId,jdbcType=VARCHAR}," +
                    " #{record.config,jdbcType=VARCHAR}," +
                    " #{record.parentId,jdbcType=VARCHAR}," +
                    " #{record.createBy,jdbcType=VARCHAR}," +
                    " #{record.createTime,jdbcType=TIMESTAMP}, " +
                    " #{record.updateBy,jdbcType=VARCHAR}, " +
                    "#{record.updateTime,jdbcType=TIMESTAMP}",
            "	</trim>" +
                    "</foreach>",
            "</script>",
    })
    void batchInsert(List<Widget> widgets);

    @Update({
            "<script>",
            "<foreach collection='widgets' item='record' index='index' separator=';'>",
            "UPDATE widget SET ",
            " config = #{record.config,jdbcType=LONGVARCHAR},",
            " parent_id = #{record.parentId,jdbcType=LONGVARCHAR},",
            " update_by   = #{record.updateBy,jdbcType=VARCHAR},",
            " update_time = #{record.updateTime,jdbcType=TIMESTAMP} ",
            "WHERE id = #{record.id,jdbcType=VARCHAR}",
            "</foreach>",
            "</script>",
    })
    void batchUpdate(List<Widget> widgets);

    @Delete({
            "<script>",
            "DELETE FROM rel_widget_element WHERE widget_id IN " +
                    "<foreach collection='widgetIds' item='item' index='index' open='(' close=')' separator=','>  #{item} </foreach>;",
            "DELETE  FROM rel_widget_widget WHERE source_id IN " +
                    "<foreach collection='widgetIds' item='item' index='index' open='(' close=')' separator=','>  #{item} </foreach> " +
                    " OR target_id IN " +
                    "<foreach collection='widgetIds' item='item' index='index' open='(' close=')' separator=','>  #{item} </foreach> ;",
            "DELETE  FROM widget WHERE id IN " +
                    "<foreach collection='widgetIds' item='item' index='index' open='(' close=')' separator=','>  #{item} </foreach> ;",
            "</script>",
    })
    int deleteWidgets(List<String> widgetIds);

}

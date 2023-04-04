package datart.core.mappers.ext;


import datart.core.entity.RelWidgetElement;
import datart.core.mappers.RelWidgetElementMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RelWidgetElementMapperExt extends RelWidgetElementMapper {

    @Select({
            "SELECT * FROM rel_widget_element WHERE widget_id=#{widgetId}"
    })
    List<RelWidgetElement> listWidgetElements(String widgetId);

    @Select({
            "<script>",
            "SELECT * FROM rel_widget_element WHERE widget_id IN " +
                    "<foreach collection='widgetIds' item='item' index='index' open='(' close=')' separator=','> " +
                    " #{item} " +
                    "</foreach>",
            "</script>"
    })
    List<RelWidgetElement> listWidgetElementsByIds(@Param("widgetIds") List<String> widgetId);

    @Insert({
            "<script>",
            "INSERT INTO rel_widget_element (id, widget_id, rel_type, rel_id) VALUES " +
                    "<foreach collection='elements' item='record' index='index' separator=','>" +
                    "	<trim prefix='(' suffix=')' suffixOverrides=','>" +
                    "		#{record.id,jdbcType=VARCHAR}," +
                    "		#{record.widgetId,jdbcType=VARCHAR}," +
                    "		#{record.relType,jdbcType=VARCHAR}," +
                    "		#{record.relId,jdbcType=VARCHAR}," +
                    "	</trim>" +
                    "</foreach>",
            "</script>",
    })
    void batchInsert(List<RelWidgetElement> elements);

    @Delete({
            "<script>",
            "DELETE FROM rel_widget_element WHERE widget_id IN " +
                    "<foreach collection='widgetIds' item='item' index='index' open='(' close=')' separator=','> " +
                    " #{item} " +
                    "</foreach>",
            "</script>"
    })
    int deleteByWidgets(@Param("widgetIds") List<String> widgetIds);

}

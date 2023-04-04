package datart.core.mappers.ext;

import datart.core.entity.RelWidgetWidget;
import datart.core.mappers.RelWidgetWidgetMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RelWidgetWidgetMapperExt extends RelWidgetWidgetMapper {

    @Insert({
            "<script>",
            "INSERT INTO rel_widget_widget (id, source_id, target_id, config) " +
                    "VALUES " +
                    "<foreach collection='relWidgetWidgets' item='record' index='index' separator=','>" +
                    "	<trim prefix='(' suffix=')' suffixOverrides=','>" +
                    "		#{record.id,jdbcType=VARCHAR}," +
                    "		#{record.sourceId,jdbcType=VARCHAR}," +
                    "		#{record.targetId,jdbcType=VARCHAR}," +
                    "		#{record.config,jdbcType=LONGVARCHAR}" +
                    "	</trim>" +
                    "</foreach>",
            "</script>",
    })
    int batchInsert(List<RelWidgetWidget> relWidgetWidgets);


    @Update({
            "<script>",
            "<foreach collection='relWidgetWidgets' item='record' index='index' separator=';'>" +
                    "UPDATE rel_widget_widget " +
                    "SET config      = #{record.config,jdbcType=LONGVARCHAR}," +
                    "    sourceId   = #{record.sourceId,jdbcType=VARCHAR}," +
                    "    targetId = #{record.targetId,jdbcType=VARCHAR} " +
                    "WHERE id = #{record.id,jdbcType=VARCHAR}",
            "</foreach>",
            "</script>",
    })
    int batchUpdate(List<RelWidgetWidget> relWidgetWidgets);

    @Delete({
            "<script>",
            "DELETE  FROM rel_widget_widget WHERE id IN " +
                    "<foreach collection='relIds' item='item' index='index' open='(' close=')' separator=','>  #{item} </foreach> ;",
            "</script>",
    })
    int batchDelete(List<String> relIds);

    @Delete({
            "DELETE FROM rel_widget_widget WHERE source_id=#{sourceId}"
    })
    int deleteBySourceId(String sourceId);

    @Select({
            "SELECT rww.* " +
                    "FROM rel_widget_widget rww " +
                    "WHERE rww.source_id = #{sourceId}"
    })
    List<RelWidgetWidget> listTargetWidgets(String sourceId);

    @Select({
            "<script>",
            "SELECT rww.* " +
                    "FROM rel_widget_widget rww " +
                    "WHERE rww.source_id IN " +
                    "<foreach collection='sourceIds' item='item' index='index' open='(' close=')' separator=','>  #{item} </foreach> ;",
            "</script>",
    })
    List<RelWidgetWidget> listTargetWidgetsByIds(List<String> sourceIds);

}

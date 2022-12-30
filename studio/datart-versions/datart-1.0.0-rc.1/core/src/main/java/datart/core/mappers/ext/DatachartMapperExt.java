package datart.core.mappers.ext;

import datart.core.entity.Datachart;
import datart.core.mappers.DatachartMapper;
import org.apache.ibatis.annotations.CacheNamespaceRef;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

@Mapper
@CacheNamespaceRef(value = FolderMapperExt.class)
public interface DatachartMapperExt extends DatachartMapper {

    @Select({
            "SELECT * FROM datachart WHERE org_id=#{orgId} AND `status`=0"
    })
    List<Datachart> listArchived(String orgId);

    @Select({
            "<script>",
            "SELECT * FROM `datachart` WHERE `status`!= 0 AND `id` IN ",
            "<foreach collection='datachartIds' item='item' index='index' open='(' close=')' separator=','>  #{item} </foreach> ;",
            "</script>"
    })
    List<Datachart> listByIds(Set<String> datachartIds);

    @Select({
            "SELECT",
            "COUNT(*)",
            "FROM",
            "rel_widget_element rwe ",
            "JOIN widget w ON w.id = rwe.widget_id ",
            "JOIN dashboard d ON w.dashboard_id = d.id  ",
            "AND d.`status` !=0 ",
            "WHERE ",
            "rwe.rel_type = 'DATACHART'  ",
            "AND rwe.rel_id = #{datachartId}"
    })
    int countWidgetRels(String datachartId);
    
}

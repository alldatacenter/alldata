package datart.core.mappers.ext;

import datart.core.entity.View;
import datart.core.mappers.ViewMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

@Mapper
public interface ViewMapperExt extends ViewMapper {


    @Select({
            "SELECT id,`name`,org_id,`index`,`type`, is_folder,parent_id,source_id,description FROM view WHERE org_id=#{orgId} AND `status`=1 ORDER BY create_time ASC "
    })
    List<View> listByOrgId(String orgId);

    @Select({
            "SELECT v.* from view v where v.status=1 AND v.id=#{id}"
    })
    View selectActiveByPrimaryKey(@Param("id") String id);

    @Select({
            "SELECT * FROM `view` WHERE org_id=#{orgId} AND `status`=0"
    })
    List<View> listArchived(String orgId);

    @Select({
            "<script>",
            "SELECT * FROM `view` WHERE `status`= 1 AND `id` IN ",
            "<foreach collection='viewIds' item='item' index='index' open='(' close=')' separator=','>  #{item} </foreach> ;",
            "</script>"
    })
    List<View> listByIds(Set<String> viewIds);

    @Delete({
            "DELETE FROM view where id = #{viewId};",
            "DELETE FROM rel_subject_columns where view_id = #{viewId};",
            "DELETE FROM rel_variable_subject rvs where variable_id IN (SELECT id from variable where view_id = #{view_id});",
            "DELETE FROM variable where view_id = #{viewId};",
    })
    int deleteAll(String viewId);

    @Select({
            "SELECT COUNT(*) FROM `view` WHERE parent_id = #{viewId} AND `status`!=0"
    })
    int checkReference(String viewId);

    @Select({
            "<script>",
            "SELECT * FROM  `view`  WHERE org_id=#{orgId} AND `name` = #{name}",
            "<if test=\"parentId==null\">",
            " AND parent_id IS NULL ",
            "</if>",
            "<if test=\"parentId!=null\">",
            " AND parent_id=#{parentId} ",
            "</if>",
            "</script>",
    })
    List<View> checkName(String orgId, String parentId, String name);

}

package datart.core.mappers.ext;

import datart.core.entity.Schedule;
import datart.core.mappers.ScheduleMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ScheduleMapperExt extends ScheduleMapper {

    @Select({
            "SELECT id,`name`,org_id,type,`active`,parent_id,is_folder,`index` FROM `schedule` s WHERE s.org_id=#{orgId} AND `status`=1 ORDER BY create_time ASC"
    })
    List<Schedule> selectByOrg(String orgId);

    @Select({
            "SELECT id,`name`,org_id,type,`active`,parent_id,is_folder,`index` FROM `schedule` s WHERE s.org_id=#{orgId} AND `status`=0 ORDER BY create_time ASC"
    })
    List<Schedule> selectArchived(String orgId);

    @Select({
            "SELECT COUNT(*) FROM `schedule` WHERE parent_id = #{scheduleId} AND `status`!=0"
    })
    int checkReference(String scheduleId);

    @Select({
            "<script>",
            "SELECT * FROM  `schedule`  WHERE org_id=#{orgId} AND `name` = #{name}",
            "<if test=\"parentId==null\">",
            " AND parent_id IS NULL ",
            "</if>",
            "<if test=\"parentId!=null\">",
            " AND parent_id=#{parentId} ",
            "</if>",
            "</script>",
    })
    List<Schedule> checkName(String orgId, String name, String parentId);

}

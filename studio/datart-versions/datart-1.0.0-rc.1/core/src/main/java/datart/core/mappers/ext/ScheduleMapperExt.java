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

}

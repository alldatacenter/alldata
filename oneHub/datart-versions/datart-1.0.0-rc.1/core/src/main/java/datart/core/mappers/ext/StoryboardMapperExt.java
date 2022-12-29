package datart.core.mappers.ext;

import datart.core.entity.Storyboard;
import datart.core.mappers.StoryboardMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StoryboardMapperExt extends StoryboardMapper {

    @Select({
            "SELECT * FROM storyboard t WHERE t.org_id=#{orgId} AND t.`name`=#{name}"
    })
    List<Storyboard> selectByOrgAndName(String orgId, String name);

    @Select({
            "SELECT * FROM storyboard WHERE org_id=#{orgId} AND `status`=0"
    })
    List<Storyboard> listArchived(String orgId);

    @Select({
            "SELECT * FROM storyboard t WHERE t.org_id=#{orgId} AND `status`!=0"
    })
    List<Storyboard> selectByOrg(String orgId);

}

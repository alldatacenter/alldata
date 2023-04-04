package datart.core.mappers.ext;

import datart.core.entity.Organization;
import datart.core.entity.Source;
import datart.core.mappers.SourceMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


import java.util.List;


@Mapper
public interface SourceMapperExt extends SourceMapper {

    @Select({
            "<script>",
            "SELECT * FROM source  WHERE org_id=#{orgId} AND `name` = #{name}",
            "<if test=\"parentId==null\">",
            " AND parent_id IS NULL ",
            "</if>",
            "<if test=\"parentId!=null\">",
            " AND parent_id=#{parentId} ",
            "</if>",
            "</script>",
    })
    List<Source> checkName(String orgId, String parentId, String name);

    @Select({
            "SELECT s.* FROM source s WHERE s.status=#{archived} and s.org_id=#{orgId} ORDER BY create_time ASC"
    })
    List<Source> listByOrg(@Param("orgId") String orgId, boolean archived);

    @Select({
            "SELECT org.* FROM organization org JOIN source s " +
                    "ON s.orgId=org.od AND s.id=#{sourceId}"
    })
    Organization getOrgById(@Param("sourceId") String sourceId);

    @Select({
            "SELECT COUNT(*) FROM `source` WHERE parent_id = #{sourceId} AND `status`!=0"
    })
    int checkReference(String sourceId);

}

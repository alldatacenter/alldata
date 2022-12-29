package datart.core.mappers.ext;

import datart.core.entity.RelUserOrganization;
import datart.core.mappers.RelUserOrganizationMapper;
import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RelUserOrganizationMapperExt extends RelUserOrganizationMapper {

    @Select({
            "SELECT " +
                    "	ruo.* " +
                    "FROM " +
                    "	rel_user_organization ruo " +
                    "WHERE " +
                    "	ruo.user_id = #{userId} AND ruo.org_id=#{orgId}"
    })
    RelUserOrganization selectByUserAndOrg(@Param("userId") String userId, @Param("orgId") String orgId);

}

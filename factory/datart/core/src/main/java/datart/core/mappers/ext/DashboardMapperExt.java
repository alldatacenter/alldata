package datart.core.mappers.ext;

import datart.core.entity.Dashboard;
import datart.core.mappers.DashboardMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
@CacheNamespaceRef(value = FolderMapperExt.class)
public interface DashboardMapperExt extends DashboardMapper {

    @Select({
            "SELECT " +
                    "	d.id, " +
                    "	d.`index`, " +
                    "	d.is_folder, " +
                    "	d.`name`, " +
                    "	d.parent_id, " +
                    "	d.portal_id " +
                    "FROM " +
                    "	dashboard d " +
                    "WHERE " +
                    "	d.`status` != 0 " +
                    "AND d.org_id = #{orgId}"
    })
    List<Dashboard> listByOrgId(@Param("orgId") String portalId);

    @Select({
            "SELECT id,`name`,org_id,`status` FROM dashboard WHERE org_id=#{orgId} AND `status`=0"
    })
    List<Dashboard> listArchived(String orgId);

    @Delete({
            "DELETE FROM dashboard WHERE id = #{dashboardId};",
            "DELETE FROM rel_widget_widget WHERE source_id in (SELECT DISTINCT id FROM widget WHERE dashboard_id=#{dashboardId});",
            "DELETE FROM rel_widget_widget WHERE target_id in (SELECT DISTINCT id FROM widget WHERE dashboard_id=#{dashboardId});",
            "DELETE FROM rel_widget_element WHERE widget_id in (SELECT DISTINCT id FROM widget WHERE dashboard_id=#{dashboardId});",
            "DELETE FROM widget WHERE dashboard_id=#{dashboardId};",
    })
    int deleteDashboard(String dashboardId);

}
package datart.server.config;

import datart.core.base.consts.TenantManagementMode;
import datart.core.base.exception.Exceptions;
import datart.core.common.Application;
import datart.core.entity.Organization;
import datart.core.mappers.ext.OrganizationMapperExt;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class AppModeStartConfig implements ApplicationListener<ApplicationStartedEvent> {

    private final OrganizationMapperExt organizationMapper;

    public AppModeStartConfig(OrganizationMapperExt organizationMapper) {
        this.organizationMapper = organizationMapper;
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent applicationStartedEvent) {
        TenantManagementMode currMode = Application.getCurrMode();
        if (TenantManagementMode.TEAM.equals(currMode)) {
            List<Organization> organizations = organizationMapper.list();
            int orgCount = CollectionUtils.size(organizations);
            if (orgCount > 1) {
                Exceptions.base("There is more than one organization in team tenant-management-mode, please initialize database or switch to platform tenant-management-mode.");
            }
        }
        log.info("The application is running in {} tenant-management-mode.", currMode);
    }

}

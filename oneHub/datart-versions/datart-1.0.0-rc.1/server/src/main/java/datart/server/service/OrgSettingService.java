package datart.server.service;

import datart.core.entity.OrgSettings;
import datart.core.mappers.ext.OrgSettingsMapperExt;

import java.util.List;

public interface OrgSettingService extends BaseCRUDService<OrgSettings, OrgSettingsMapperExt> {

    List<OrgSettings> listOrgSettings(String orgId);

    Integer getDownloadRecordLimit(String orgId);

    boolean setDownloadRecordLimit(String orgId);

}

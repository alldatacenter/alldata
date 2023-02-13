/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package datart.server.service.impl;

import datart.core.base.consts.PlatformSettings;
import datart.core.entity.OrgSettings;
import datart.core.mappers.ext.OrgSettingsMapperExt;
import datart.server.service.BaseService;
import datart.server.service.OrgSettingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrgSettingServiceImpl extends BaseService implements OrgSettingService {

    @Value("${datart.download.limit:1000000}")
    private Integer defaultLimit;

    private final OrgSettingsMapperExt orgSettingsMapper;

    public OrgSettingServiceImpl(OrgSettingsMapperExt orgSettingsMapper) {
        this.orgSettingsMapper = orgSettingsMapper;
    }


    @Override
    public void requirePermission(OrgSettings entity, int permission) {

    }

    @Override
    public List<OrgSettings> listOrgSettings(String orgId) {
        return orgSettingsMapper.selectByOrg(orgId);
    }

    @Override
    public Integer getDownloadRecordLimit(String orgId) {
        OrgSettings orgSettings = orgSettingsMapper.selectByOrgAndType(orgId, PlatformSettings.DOWNLOAD_RECORD_LIMIT.name());
        if (orgSettings == null) {
            return defaultLimit;
        }
        try {
            return Integer.parseInt(orgSettings.getConfig());
        } catch (Exception e) {
            return defaultLimit;
        }
    }

    @Override
    public boolean setDownloadRecordLimit(String orgId) {
        return false;
    }
}

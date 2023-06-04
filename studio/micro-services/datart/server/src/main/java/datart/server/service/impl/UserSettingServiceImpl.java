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

import datart.core.common.UUIDGenerator;
import datart.core.entity.UserSettings;
import datart.core.mappers.ext.UserSettingsMapperExt;
import datart.server.base.params.BaseCreateParam;
import datart.server.service.BaseService;
import datart.server.service.UserSettingService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class UserSettingServiceImpl extends BaseService implements UserSettingService {

    private final UserSettingsMapperExt userSettingsMapper;

    public UserSettingServiceImpl(UserSettingsMapperExt userSettingsMapper) {
        this.userSettingsMapper = userSettingsMapper;
    }

    @Override
    public List<UserSettings> listUserSettings() {
        return userSettingsMapper.selectByUser(getCurrentUser().getId());
    }

    @Override
    public boolean deleteByUserId(String userId) {
        return userSettingsMapper.deleteByUser(userId)>0;
    }

    @Override
    @Transactional
    public UserSettings create(BaseCreateParam createParam) {
        UserSettings userSettings = new UserSettings();
        BeanUtils.copyProperties(createParam, userSettings);
        userSettings.setUserId(getCurrentUser().getId());
        userSettings.setCreateBy(getCurrentUser().getId());
        userSettings.setCreateTime(new Date());
        userSettings.setId(UUIDGenerator.generate());
        getDefaultMapper().insert(userSettings);
        return userSettings;
    }

    @Override
    public void requirePermission(UserSettings entity, int permission) {

    }
}

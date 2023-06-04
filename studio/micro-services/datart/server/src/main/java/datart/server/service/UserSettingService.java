package datart.server.service;

import datart.core.entity.UserSettings;
import datart.core.mappers.ext.UserSettingsMapperExt;

import java.util.List;

public interface UserSettingService extends BaseCRUDService<UserSettings, UserSettingsMapperExt> {

    List<UserSettings> listUserSettings();

    boolean deleteByUserId(String userId);

}

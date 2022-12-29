/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { request2 } from 'utils/request';
import { UserSettingTypes } from '../constants';
import { UserSetting } from './types';

export function findLvoSetting(
  userSettings: UserSetting[],
): undefined | UserSetting {
  return userSettings.find(
    ({ relType }) => relType === UserSettingTypes.LastVisitedOrganization,
  );
}

export const updateLvoUserSettings = async (
  userSettings: undefined | UserSetting[],
  orgId?: string,
): Promise<UserSetting[]> => {
  if (userSettings) {
    const lvoSetting = findLvoSetting(userSettings);

    if (lvoSetting) {
      if (!orgId) {
        await request2<boolean>({
          url: `/settings/user/${lvoSetting.id}`,
          method: 'DELETE',
        });
        return userSettings.filter(({ id }) => id !== lvoSetting.id);
      } else {
        const updated = { ...lvoSetting, relId: orgId };
        await request2<UserSetting>({
          url: `/settings/user/${lvoSetting.id}`,
          method: 'PUT',
          data: updated,
        });
        return userSettings.map(s => (s.id === updated.id ? updated : s));
      }
    } else {
      const { data } = await request2<UserSetting>({
        url: '/settings/user',
        method: 'POST',
        data: {
          relId: orgId,
          relType: UserSettingTypes.LastVisitedOrganization,
          config: null,
        },
      });
      return userSettings.concat(data);
    }
  }
  return [];
};

export const isParentIdEqual = function (
  prevParentId: string | null,
  nextParentId: string | null | undefined,
) {
  /* eslint-disable */
  return prevParentId != nextParentId;
  /* eslint-disable */
  //prevParentId有可能是null nextParentId有可能是undefined 所以这里用双等号。 Prevparentid may be null and nextparentid may be undefined, so the double equal sign is used here.
};

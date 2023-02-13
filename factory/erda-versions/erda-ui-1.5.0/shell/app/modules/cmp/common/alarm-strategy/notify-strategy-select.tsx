// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.
import React from 'react';
import { map, find } from 'lodash';
import i18n from 'i18n';
import { ErdaIcon } from 'common';
import { Select, Divider } from 'antd';
import { WithAuth } from 'user/common';
import { notifyChannelOptionsMap } from 'application/pages/settings/components/app-notify/common-notify-group';

const { Option } = Select;

interface IProps {
  id: string;
  handleEditNotifyStrategy: (id: string, item: { key: string; value: string }) => void;
  valueOptions: Array<{ key: string; display: string }>;
  addNotificationGroupAuth: boolean;
  goToNotifyGroup: () => void;
  notifyGroups: COMMON_NOTIFY.INotifyGroup[];
  alertLevelOptions: Array<{ key: string; display: string }>;
  notifyChannelMap: typeof notifyChannelOptionsMap;
  updater: (groupId: number) => void;
  handleRemoveNotifyStrategy: (id: string) => void;
}

export const NotifyStrategySelect = ({
  id,
  current,
  handleEditNotifyStrategy,
  valueOptions,
  updater,
  addNotificationGroupAuth,
  goToNotifyGroup,
  notifyGroups,
  alertLevelOptions,
  notifyChannelMap,
  handleRemoveNotifyStrategy,
}: IProps) => {
  return (
    <div className="flex items-center mb-4 last:mb-0">
      <Select
        className="mr-8"
        style={{ width: 180 }}
        placeholder={i18n.t('cmp:select group')}
        value={current?.groupId}
        onSelect={(groupId: number) => {
          updater(groupId);
          handleEditNotifyStrategy(id, { key: 'groupId', value: groupId });
          const activeGroup = find(notifyGroups, (item) => item.id === groupId);
          const groupTypeOptions =
            (activeGroup && notifyChannelMap[activeGroup.targets[0].type]).map((x) => ({
              key: x.value,
              display: x.name,
            })) || [];

          handleEditNotifyStrategy(id, {
            key: 'groupTypeOptions',
            value: groupTypeOptions,
          });

          handleEditNotifyStrategy(id, { key: 'groupType', value: [groupTypeOptions?.[0]?.key] });
        }}
        dropdownRender={(menu) => (
          <div>
            {menu}
            <Divider className="my-1" />
            <div className="text-xs px-2 py-1 text-desc" onMouseDown={(e) => e.preventDefault()}>
              <WithAuth pass={addNotificationGroupAuth}>
                <span className="hover-active" onClick={goToNotifyGroup}>
                  {i18n.t('cmp:add more notification groups')}
                </span>
              </WithAuth>
            </div>
          </div>
        )}
      >
        {map(notifyGroups, (item) => (
          <Option key={item.id} value={item.id}>
            {item.name}
          </Option>
        ))}
      </Select>

      <Select
        className="mr-8"
        style={{ width: 280 }}
        placeholder={i18n.t('cmp:select level')}
        value={current?.level}
        onChange={(value) => handleEditNotifyStrategy(id, { key: 'level', value })}
        mode="multiple"
      >
        {map(alertLevelOptions, (item) => {
          return (
            <Option key={item.key} value={item.key}>
              {item.display}
            </Option>
          );
        })}
      </Select>
      <Select
        placeholder={i18n.t('cmp:select notify method')}
        style={{ width: 280 }}
        value={current?.groupType}
        onChange={(value) => handleEditNotifyStrategy(id, { key: 'groupType', value })}
        mode="multiple"
        disabled={valueOptions?.length === 0}
      >
        {map(valueOptions, (item) => {
          return (
            <Option key={item?.key} value={item?.key}>
              {item?.display}
            </Option>
          );
        })}
      </Select>
      <ErdaIcon
        type="reduce"
        className="cursor-pointer ml-2 text-darkgray hover:text-primary"
        size="20"
        onClick={() => handleRemoveNotifyStrategy(id)}
      />
    </div>
  );
};

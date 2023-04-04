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

import { Checkbox, Space } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { memo, useCallback, useEffect, useState } from 'react';
import {
  PermissionLevels,
  ResourceTypes,
  RESOURCE_TYPE_PERMISSION_MAPPING,
  SubjectTypes,
  Viewpoints,
  VizResourceSubTypes,
} from '../../constants';
import { DataSourceTreeNode } from '../../slice/types';
import { getDefaultPermissionArray } from '../../utils';
import { getChangedPermission, getPrivilegeSettingType } from './utils';

interface PrivilegeSettingProps {
  record: DataSourceTreeNode;
  viewpoint: Viewpoints;
  viewpointType: ResourceTypes | SubjectTypes;
  dataSourceType: ResourceTypes | SubjectTypes;
  vizSubTypes?: VizResourceSubTypes;
  onChange: (
    record: DataSourceTreeNode,
    newPermissionArray: PermissionLevels[],
    index: number,
    base: PermissionLevels,
  ) => void;
}

export const PrivilegeSetting = memo(
  ({
    record,
    viewpoint,
    viewpointType,
    dataSourceType,
    vizSubTypes,
    onChange,
  }: PrivilegeSettingProps) => {
    const [values, setValues] = useState<PermissionLevels[]>(
      getDefaultPermissionArray(vizSubTypes),
    );
    const t = useI18NPrefix('permission');

    const resourceType = getPrivilegeSettingType(
      viewpoint,
      viewpointType,
      dataSourceType,
    );

    const privilegeChange = useCallback(
      (index, changedValue) => e => {
        const changedValues = getChangedPermission(
          !e.target.checked,
          values,
          index,
          changedValue,
        );
        onChange(record, changedValues, index, changedValue);
      },
      [record, values, onChange],
    );

    useEffect(() => {
      setValues(record.permissionArray);
    }, [record]);
    return (
      <Space>
        {RESOURCE_TYPE_PERMISSION_MAPPING[
          resourceType! + (vizSubTypes || '')
        ].map((level, index) => {
          return (
            <Checkbox
              key={PermissionLevels[level]}
              checked={level === values[index]}
              onChange={privilegeChange(index, level)}
            >
              {t(
                `privilegeLabel.${(
                  resourceType! + (vizSubTypes || '')
                ).toLowerCase()}.${index}`,
              )}
            </Checkbox>
          );
        })}
      </Space>
    );
  },
);

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

import { Card } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { memo, useCallback, useEffect, useMemo, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { ResourceTypes, SubjectTypes, Viewpoints } from '../constants';
import {
  selectMemberListLoading,
  selectMembers,
  selectPermissionLoading,
  selectRoleListLoading,
  selectRoles,
} from '../slice/selectors';
import { getDataSource, getResourcePermission } from '../slice/thunks';
import { getInverseViewpoints } from '../utils';
import { PermissionForm } from './PermissionForm';

interface SubjectPermissionSettingProps {
  viewpoint: Viewpoints;
  viewpointType: ResourceTypes | SubjectTypes;
  viewpointId: string;
  orgId: string;
}

export const SubjectPermissionSetting = memo(
  ({
    viewpoint,
    viewpointType,
    viewpointId,
    orgId,
  }: SubjectPermissionSettingProps) => {
    const [tabActiveKey, setTabActiveKey] = useState<SubjectTypes>(
      SubjectTypes.Role,
    );
    const dispatch = useDispatch();
    const roles = useSelector(selectRoles);
    const members = useSelector(selectMembers);
    const roleListLoading = useSelector(selectRoleListLoading);
    const memberListLoading = useSelector(selectMemberListLoading);
    const permissionLoading = useSelector(state =>
      selectPermissionLoading(state, { viewpoint }),
    );
    const t = useI18NPrefix('permission');

    useEffect(() => {
      if (viewpointType && viewpointId) {
        dispatch(
          getResourcePermission({
            orgId: orgId,
            type: viewpointType as ResourceTypes,
            id: viewpointId,
          }),
        );
      }
    }, [dispatch, orgId, viewpointType, viewpointId]);

    useEffect(() => {
      dispatch(
        getDataSource({
          viewpoint: getInverseViewpoints(viewpoint),
          dataSourceType: tabActiveKey,
        }),
      );
    }, [dispatch, tabActiveKey, viewpoint, orgId]);

    const tabChange = useCallback(activeKey => {
      setTabActiveKey(activeKey);
    }, []);

    const tabSource = useMemo(
      () => [
        {
          type: SubjectTypes.Role,
          label: t('role'),
          dataSource: roles,
          loading: roleListLoading,
        },
        {
          type: SubjectTypes.UserRole,
          label: t('member'),
          dataSource: members,
          loading: memberListLoading,
        },
      ],
      [roles, members, roleListLoading, memberListLoading, t],
    );

    const tabList = useMemo(
      () => tabSource.map(({ type, label }) => ({ key: type, tab: label })),
      [tabSource],
    );

    return (
      <Card
        tabList={tabList}
        defaultActiveTabKey={tabActiveKey}
        tabProps={{ size: 'middle' }}
        onTabChange={tabChange}
      >
        {tabSource.map(({ type, dataSource, loading }) => (
          <PermissionForm
            key={type}
            selected={type === tabActiveKey}
            viewpoint={viewpoint}
            viewpointType={viewpointType}
            viewpointId={viewpointId}
            orgId={orgId}
            dataSource={dataSource}
            dataSourceType={type}
            permissionLoading={permissionLoading}
            resourceLoading={loading}
          />
        ))}
      </Card>
    );
  },
);

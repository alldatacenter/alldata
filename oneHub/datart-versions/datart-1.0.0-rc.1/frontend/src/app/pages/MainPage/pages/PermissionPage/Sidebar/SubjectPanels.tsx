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

import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { memo, useMemo } from 'react';
import { useSelector } from 'react-redux';
import { SubjectTypes } from '../constants';
import {
  selectMemberListLoading,
  selectMembers,
  selectRoleListLoading,
  selectRoles,
} from '../slice/selectors';
import { FlexCollapse } from './FlexCollapse';
import { SubjectList } from './SubjectList';
import { PanelsProps } from './types';
const { Panel } = FlexCollapse;

export const SubjectPanels = memo(
  ({ viewpointId, viewpointType, onToggle, onToDetail }: PanelsProps) => {
    const roles = useSelector(selectRoles);
    const members = useSelector(selectMembers);
    const roleListLoading = useSelector(selectRoleListLoading);
    const memberListLoading = useSelector(selectMemberListLoading);
    const t = useI18NPrefix('permission');

    const subjectPanels = useMemo(
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

    return (
      <FlexCollapse defaultActiveKeys={viewpointType && [viewpointType]}>
        {subjectPanels.map(
          ({ type: subjectType, label, dataSource, loading }) => (
            <Panel
              key={subjectType}
              id={subjectType}
              title={label}
              onChange={onToggle}
            >
              <SubjectList
                viewpointId={viewpointId}
                viewpointType={viewpointType as SubjectTypes}
                dataSource={dataSource}
                loading={loading}
                onToDetail={onToDetail}
              />
            </Panel>
          ),
        )}
      </FlexCollapse>
    );
  },
);

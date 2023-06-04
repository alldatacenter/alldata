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
  selectFolderListLoading,
  selectFolders,
  selectPermissionLoading,
  selectScheduleListLoading,
  selectSchedules,
  selectSourceListLoading,
  selectSources,
  selectStoryboardListLoading,
  selectStoryboards,
  selectViewListLoading,
  selectViews,
} from '../slice/selectors';
import { getDataSource, getSubjectPermission } from '../slice/thunks';
import { getInverseViewpoints } from '../utils';
import { PermissionForm } from './PermissionForm';
import { VizPermissionForm } from './PermissionForm/VizPermissionForm';

interface ResourcesPermissionSettingProps {
  viewpoint: Viewpoints;
  viewpointType: ResourceTypes | SubjectTypes;
  viewpointId: string;
  orgId: string;
}

export const ResourcesPermissionSetting = memo(
  ({
    viewpoint,
    viewpointType,
    viewpointId,
    orgId,
  }: ResourcesPermissionSettingProps) => {
    const [tabActiveKey, setTabActiveKey] = useState<ResourceTypes>(
      ResourceTypes.Viz,
    );
    const dispatch = useDispatch();
    const folders = useSelector(selectFolders);
    const storyboards = useSelector(selectStoryboards);
    const views = useSelector(selectViews);
    const sources = useSelector(selectSources);
    const schedules = useSelector(selectSchedules);
    const folderListLoading = useSelector(selectFolderListLoading);
    const storyboardListLoading = useSelector(selectStoryboardListLoading);
    const viewListLoading = useSelector(selectViewListLoading);
    const sourceListLoading = useSelector(selectSourceListLoading);
    const scheduleListLoading = useSelector(selectScheduleListLoading);
    const permissionLoading = useSelector(state =>
      selectPermissionLoading(state, { viewpoint }),
    );
    const t = useI18NPrefix('permission');

    useEffect(() => {
      if (viewpointType && viewpointId) {
        dispatch(
          getSubjectPermission({
            orgId: orgId,
            type: viewpointType as SubjectTypes,
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
          type: ResourceTypes.Viz,
          dataSource: void 0,
          loading: false,
        },
        {
          type: ResourceTypes.View,
          dataSource: views,
          loading: viewListLoading,
        },
        {
          type: ResourceTypes.Source,
          dataSource: sources,
          loading: sourceListLoading,
        },
        {
          type: ResourceTypes.Schedule,
          dataSource: schedules,
          loading: scheduleListLoading,
        },
      ],
      [
        views,
        viewListLoading,
        sources,
        sourceListLoading,
        schedules,
        scheduleListLoading,
      ],
    );

    const tabList = useMemo(
      () =>
        tabSource.map(({ type }) => ({
          key: type,
          tab: t(`module.${type.toLowerCase()}`),
        })),
      [tabSource, t],
    );

    return (
      <Card
        tabList={tabList}
        defaultActiveTabKey={tabActiveKey}
        tabProps={{ size: 'middle' }}
        onTabChange={tabChange}
      >
        {tabSource.map(({ type, dataSource, loading }) => {
          if (type === ResourceTypes.Viz) {
            return (
              <VizPermissionForm
                key={type}
                selected={type === tabActiveKey}
                viewpoint={viewpoint}
                viewpointType={viewpointType}
                viewpointId={viewpointId}
                orgId={orgId}
                dataSourceType={type}
                folders={folders}
                storyboards={storyboards}
                folderListLoading={folderListLoading}
                storyboardListLoading={storyboardListLoading}
                permissionLoading={permissionLoading}
              />
            );
          } else {
            return (
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
            );
          }
        })}
      </Card>
    );
  },
);

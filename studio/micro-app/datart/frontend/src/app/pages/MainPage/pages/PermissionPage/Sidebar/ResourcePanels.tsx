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

import { Col, Radio, Row } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import classNames from 'classnames';
import { memo, useCallback, useMemo, useState } from 'react';
import { useSelector } from 'react-redux';
import styled from 'styled-components/macro';
import { SPACE_MD, SPACE_XS } from 'styles/StyleConstants';
import { ResourceTypes } from '../constants';
import {
  selectFolderListLoading,
  selectFolders,
  selectScheduleListLoading,
  selectSchedules,
  selectSourceListLoading,
  selectSources,
  selectStoryboardListLoading,
  selectStoryboards,
  selectViewListLoading,
  selectViews,
} from '../slice/selectors';
import { FlexCollapse } from './FlexCollapse';
import { ResourceTree } from './ResourceTree';
import { PanelsProps } from './types';
const { Panel } = FlexCollapse;

export const ResourcePanels = memo(
  ({ viewpointType, onToggle, onToDetail }: PanelsProps) => {
    const [vizType, setVizType] = useState<'folder' | 'presentation'>('folder');
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
    const t = useI18NPrefix('permission');

    const resourcePanels = useMemo(
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
        sources,
        schedules,
        viewListLoading,
        sourceListLoading,
        scheduleListLoading,
      ],
    );

    const vizTypeChange = useCallback(e => {
      setVizType(e.target.value);
    }, []);

    return (
      <FlexCollapse defaultActiveKeys={viewpointType && [viewpointType]}>
        {resourcePanels.map(({ type: resourceType, dataSource, loading }) => (
          <Panel
            key={resourceType}
            id={resourceType}
            title={t(`module.${resourceType.toLowerCase()}`)}
            onChange={onToggle}
          >
            {resourceType === ResourceTypes.Viz ? (
              <>
                <VizTypeSwitch key="switch">
                  <Col>
                    <Radio.Group value={vizType} onChange={vizTypeChange}>
                      <Radio value="folder">{t('folder')}</Radio>
                      <Radio value="presentation">{t('presentation')}</Radio>
                    </Radio.Group>
                  </Col>
                </VizTypeSwitch>
                <VizTreeWrapper
                  key="folder"
                  className={classNames({ selected: vizType === 'folder' })}
                >
                  <ResourceTree
                    loading={folderListLoading}
                    dataSource={folders}
                    onSelect={onToDetail}
                  />
                </VizTreeWrapper>
                <VizTreeWrapper
                  key="persentation"
                  className={classNames({
                    selected: vizType === 'presentation',
                  })}
                >
                  <ResourceTree
                    loading={storyboardListLoading}
                    dataSource={storyboards}
                    onSelect={onToDetail}
                  />
                </VizTreeWrapper>
              </>
            ) : (
              <ResourceTree
                loading={loading}
                dataSource={dataSource}
                onSelect={onToDetail}
              />
            )}
          </Panel>
        ))}
      </FlexCollapse>
    );
  },
);

const VizTypeSwitch = styled(Row)`
  padding: ${SPACE_XS} ${SPACE_MD};
`;

const VizTreeWrapper = styled.div`
  display: none;

  &.selected {
    display: block;
  }
`;

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

import routeInfoStore from 'core/stores/route';
import { updateSearch } from 'common/utils';
import { Radio } from 'antd';
import moment from 'moment';
import { isEmpty, map, merge, get, startsWith } from 'lodash';
import { Holder, BoardGrid } from 'common';
import { useUpdate } from 'common/use-hooks';
import React from 'react';
import i18n from 'i18n';
import IterationSelect from 'project/common/components/issue/iteration-select';
import { getIterationDetail } from 'project/services/project-iteration';
import { getProjectInfo, getDashboard } from 'project/services/project';
import { createLoadDataFn } from 'cmp/common/custom-dashboard/data-loader';

const DashBoard = React.memo(BoardGrid.Pure);

enum DashboardType {
  BUG = 'bug',
  WORKING = 'working',
}

const RadioButton = Radio.Button;
const RadioGroup = Radio.Group;

const getDateMs = (timeString: string, isNextDay?: boolean) => {
  const time = moment(timeString, 'YYYY-MM-DD');
  return isNextDay ? time.add(1, 'd').valueOf() : time.valueOf();
};

const getTimeRange = async (projectID: number, iterationId?: number) => {
  if (iterationId && `${iterationId}` !== '-1') {
    const { data = {} } = await getIterationDetail({ id: iterationId, projectID });
    const [start, end] = [getDateMs(data.startedAt), getDateMs(data.finishedAt, true)];
    const todayMs = moment().endOf('d').valueOf();
    return [start, todayMs > start && todayMs < end ? todayMs : end];
  } else {
    const { data = {} } = await getProjectInfo(projectID);
    return [getDateMs(data.createdAt), moment().endOf('d').valueOf()];
  }
};

export const ProjectDashboard = () => {
  const [projectId, isIn] = routeInfoStore.useStore((s) => [s.params.projectId, s.isIn]);
  const iterationID = routeInfoStore.useStore((s) => s.query.iterationID);
  const [{ layout, boardData, type }, updater] = useUpdate({ layout: [], boardData: {}, type: DashboardType.BUG });

  React.useEffect(() => {
    getDashboard(type).then(({ data }: any) => updater.boardData(data));
  }, [type, updater]);

  React.useEffect(() => {
    if (projectId && boardData && boardData.viewConfig) {
      getTimeRange(+projectId, iterationID).then((timeRange) => {
        const _layout = map(boardData.viewConfig, (viewItem) => {
          const query = get(viewItem, 'view.api.query') || {};
          const { start, end } = query;
          const _viewItem = merge({}, viewItem, {
            view: {
              api: {
                url: isIn('orgCenter') // 在企业下时，使用企业前缀，否则监控权限会报错
                  ? viewItem.view.api.url.replace('/api/project', '/api/orgCenter')
                  : viewItem.view.api.url,
                query: {
                  'eq_tags.issue_iterator_id': iterationID,
                  filter_project_id: projectId,
                  align: false,
                  trans: true,
                  start: startsWith(start, '${') ? timeRange[0] : start,
                  end: startsWith(end, '${') ? timeRange[1] : end,
                },
              },
            },
          });
          const { api, chartType } = _viewItem.view;
          return merge({}, _viewItem, { view: { loadData: createLoadDataFn(api, chartType) } });
        });
        updater.layout(_layout);
      });
    }
  }, [boardData, isIn, iterationID, projectId, updater]);

  return (
    <div className="project-dashboard">
      <div className="flex justify-between items-center mb-3">
        <RadioGroup onChange={(e: any) => updater.type(e.target.value)} value={type}>
          <RadioButton value={DashboardType.BUG}>{i18n.t('bug')}</RadioButton>
          <RadioButton value={DashboardType.WORKING}>{i18n.t('dop:workload')}</RadioButton>
        </RadioGroup>
        <IterationSelect
          allowClear
          placeholder={i18n.t('dop:view by iteration')}
          value={iterationID}
          onChange={(v) => updateSearch({ iterationID: v || undefined })}
        />
      </div>

      <Holder when={isEmpty(layout)}>
        <DashBoard layout={layout} showOptions />
      </Holder>
    </div>
  );
};

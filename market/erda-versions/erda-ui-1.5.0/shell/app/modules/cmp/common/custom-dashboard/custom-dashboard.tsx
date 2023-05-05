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
import i18n from 'i18n';
import { Input, message } from 'antd';
import { map, merge, reduce, isString, get } from 'lodash';
import { IF, BoardGrid, TimeSelect } from 'common';
import { useUpdate } from 'common/use-hooks';
import { registDiceDataConfigProps, createLoadDataFn } from '@erda-ui/dashboard-configurator';
import { goTo, getTimeSpan } from 'common/utils';
import moment, { Moment } from 'moment';
import { useMount } from 'react-use';
import routeInfoStore from 'core/stores/route';
import orgMonitorMetaDataStore from 'cmp/stores/query-monitor-metadata';
import mspMonitorMetaDataStore from 'msp/query-analysis/custom-dashboard/stores/query-monitor-metadata';
import orgCustomDashboardStore from 'app/modules/cmp/stores/custom-dashboard';
import mspCustomDashboardStore from 'msp/query-analysis/custom-dashboard/stores/custom-dashboard';
import { CustomDashboardScope } from 'app/modules/cmp/stores/_common-custom-dashboard';
import { getVariableStr } from '../utils';
import { createLoadDataFn as createOldLoadDataFn } from './data-loader';
import { ITimeRange } from 'common/components/time-select/common';

const storeMap = {
  [CustomDashboardScope.ORG]: orgCustomDashboardStore,
  [CustomDashboardScope.MICRO_SERVICE]: mspCustomDashboardStore,
};

const dataConfigMetaDataStoreMap = {
  [CustomDashboardScope.ORG]: orgMonitorMetaDataStore,
  [CustomDashboardScope.MICRO_SERVICE]: mspMonitorMetaDataStore,
};

const dataUrlMap = {
  [CustomDashboardScope.ORG]: '/api/orgCenter/metrics-query',
  [CustomDashboardScope.MICRO_SERVICE]: '/api/tmc/metrics-query',
};

const urlMap = {
  [CustomDashboardScope.ORG]: goTo.pages.orgCustomDashboard,
  [CustomDashboardScope.MICRO_SERVICE]: goTo.pages.microServiceCustomDashboard,
};

export default ({ scope, scopeId }: { scope: CustomDashboardScope; scopeId: string }) => {
  registDiceDataConfigProps({
    dataConfigMetaDataStore: dataConfigMetaDataStoreMap[scope],
    scope,
    scopeId,
    loadDataApi: {
      url: dataUrlMap[scope],
      query:
        scope === 'org'
          ? undefined
          : {
              filter__metric_scope: scope,
              filter__metric_scope_id: scopeId,
            },
    },
  });

  const store = storeMap[scope];
  const timeSpan = store.useStore((s) => s.globalTimeSelectSpan.range);
  const globalTimeSelectSpan = store.getState((s) => s.globalTimeSelectSpan);
  const { createCustomDashboard, updateCustomDashboard, getCustomDashboardDetail, updateState } = store;
  const [params, query] = routeInfoStore.useStore((s) => [s.params, s.query]);
  const { dashboardId } = params;
  const isDashboardDetail = !!dashboardId;
  const isFromMsp = scope === CustomDashboardScope.MICRO_SERVICE;

  const [{ curLayout, isEditMode, dashboardName, isNewVersionDC, dashboardDesc, editorToggleStatus }, updater] =
    useUpdate({
      curLayout: [],
      isEditMode: false,
      dashboardName: '',
      dashboardDesc: '',
      isNewVersionDC: !isDashboardDetail,
      editorToggleStatus: false,
    });

  const _getCustomDashboardDetail = React.useCallback(
    (id: string) => {
      getCustomDashboardDetail({ id, scopeId }).then((customDashboardDetail: any) => {
        const { name, desc, version: _version } = customDashboardDetail;
        const _isNewVersionDC = _version === 'v2';
        updater.dashboardName(name);
        updater.isNewVersionDC(_isNewVersionDC);
        updater.dashboardDesc(desc || '');

        const { startTimeMs, endTimeMs } = timeSpan;
        const layout = map(customDashboardDetail.viewConfig, (viewItem) => {
          const filters = get(viewItem, 'view.api.extraData.filters');
          const _viewItem = merge({}, viewItem, {
            view: {
              api: {
                query: {
                  ...reduce(
                    filters,
                    (acc, { value, method, tag }) => {
                      const matchQuery = isString(value) ? getVariableStr(value) : undefined;
                      return {
                        ...acc,
                        [`${method}_${tag}`]: matchQuery ? query[matchQuery] : value.split(','),
                      };
                    },
                    {},
                  ),
                  start: startTimeMs,
                  end: endTimeMs,
                  // 初始化大盘时，后端初始不了鉴权参数
                  filter_terminus_key: isFromMsp ? scopeId : undefined,
                },
              },
            },
          });
          const { api, chartType } = _viewItem.view;

          return merge({}, _viewItem, {
            view: {
              loadData: _isNewVersionDC
                ? createLoadDataFn({ ..._viewItem.view, ...(get(_viewItem, 'view.config.dataSourceConfig') || {}) })
                : createOldLoadDataFn(api, chartType),
              config: {
                optionProps: {
                  isMoreThanOneDay: moment(endTimeMs).diff(moment(startTimeMs), 'days') > 0,
                },
              },
            },
          });
        });
        updater.curLayout(layout);
      });
    },
    [getCustomDashboardDetail, scopeId, updater, timeSpan, isFromMsp, query],
  );

  React.useEffect(() => {
    isDashboardDetail && _getCustomDashboardDetail(dashboardId);
  }, [_getCustomDashboardDetail, dashboardId, isDashboardDetail]);

  const beforeHandleSave = () => {
    if (!dashboardName) {
      message.warning(i18n.t('cmp:please input dashboard name'));
      return false;
    }
    return true;
  };

  const handleChange = (data: ITimeRange, range: Moment[]) => {
    const triggerTime = Date.now();
    const span = getTimeSpan(range);
    updateState({
      globalTimeSelectSpan: {
        data,
        range: {
          triggerTime,
          ...span,
        },
      },
    });
  };

  const handleSave = (viewConfig: any) => {
    updater.isEditMode(false);
    if (isDashboardDetail) {
      updateCustomDashboard({
        viewConfig,
        name: dashboardName,
        desc: dashboardDesc,
        scope,
        scopeId,
        id: dashboardId,
        version: 'v2',
      }).then(() => _getCustomDashboardDetail(dashboardId));
    } else {
      createCustomDashboard({
        viewConfig,
        name: dashboardName,
        desc: dashboardDesc,
        scope,
        scopeId,
        version: 'v2',
      }).then(() => goTo(urlMap[scope], params));
    }
  };

  return (
    <div className="custom-dashboard flex flex-col h-full">
      <IF check={!editorToggleStatus}>
        <div className="header mb-3">
          {/* <Select placeholder="自动刷新间隔" style={{ width: 200 }} allowClear>
            {map(AUTO_RELOAD_OPTIONS, ({ value, name }) => <Select.Option key={value} value={value}>{name}</Select.Option>)}
          </Select> */}
          <IF check={!isEditMode}>
            <div className="flex justify-end">
              <TimeSelect defaultValue={globalTimeSelectSpan.data} onChange={handleChange} />
            </div>
          </IF>
          <IF check={isEditMode}>
            <div className="dashboard-info-editor">
              <Input
                maxLength={50}
                className="mr-4"
                style={{ width: 200 }}
                placeholder={i18n.t('cmp:please input dashboard name')}
                allowClear
                value={dashboardName}
                onChange={(e: any) => updater.dashboardName(e.target.value)}
              />
              <Input
                maxLength={200}
                style={{ width: 370 }}
                placeholder={i18n.t('cmp:please input dashboard description')}
                allowClear
                value={dashboardDesc}
                onChange={(e: any) => updater.dashboardDesc(e.target.value)}
              />
            </div>
          </IF>
        </div>
      </IF>
      <div className="flex-1 pb-3">
        <Choose>
          <When condition={isNewVersionDC}>
            <BoardGrid
              timeSpan={timeSpan}
              name={dashboardName}
              layout={curLayout}
              onEdit={() => updater.isEditMode(true)}
              beforeOnSave={beforeHandleSave}
              onSave={(viewConfig: any) => handleSave(viewConfig)}
              onCancel={() => updater.isEditMode(false)}
              onEditorToggle={(status: boolean) => updater.editorToggleStatus(status)}
            />
          </When>
          <Otherwise>
            <BoardGrid.Pure name={dashboardName} layout={curLayout} />
          </Otherwise>
        </Choose>
      </div>
    </div>
  );
};

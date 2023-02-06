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
import { map, isFunction, isEmpty } from 'lodash';
import moment from 'moment';
import { Spin, Table, Badge } from 'antd';
import { Link } from 'react-router-dom';
import { Icon as CustomIcon, Copy, IF } from 'common';
import { goTo } from 'common/utils';
import { PLAN_NAME, ENV_NAME, CATEGORY_NAME } from 'app/modules/addonPlatform/pages/common/configs';
import i18n from 'i18n';

import './detail.scss';

const addonStatusMap = {
  Progressing: <Badge status="processing" text={i18n.t('processing')} />,
  Healthy: <Badge status="success" text={i18n.t('healthy')} />,
  UnHealthy: <Badge status="warning" text={i18n.t('unhealthy')} />,
  Failed: <Badge status="error" text={i18n.t('failed')} />,
  Unknown: <Badge status="default" text={i18n.t('unknown')} />,
  Stopped: <Badge status="error" text={i18n.t('stopped')} />,
};

const refTableList = [
  {
    title: i18n.t('application'),
    dataIndex: 'applicationName',
    key: 'applicationName',
  },
  {
    title: i18n.t('application instance'),
    dataIndex: 'runtimeName',
    key: 'runtimeName',
  },
  {
    title: i18n.t('cmp:deployment details'),
    dataIndex: 'applicationId',
    key: 'applicationId',
    align: 'center' as const,
    render: (_text: string, row: { applicationId: number; projectId: number; runtimeId: number }) => {
      const { applicationId, projectId, runtimeId } = row;
      return (
        <Link to={goTo.resolve.runtimeDetailRoot({ projectId, appId: applicationId, runtimeId })}>
          <CustomIcon type="link1" />
        </Link>
      );
    },
  },
];

export const PureBaseAddonInfo = ({
  addonDetail,
  loading,
  extra,
}: {
  addonDetail: Merge<MIDDLEWARE_DASHBOARD.IBaseInfo, { addonStatus?: string }>;
  loading: boolean;
  extra: React.ReactNode;
}) => {
  const itemConfigs = [
    {
      title: i18n.t('cmp:middleware'),
      value: 'addonName',
    },
    {
      title: i18n.t('version'),
      value: 'version',
    },
    {
      title: i18n.t('type'),
      value: 'category',
      render: (category: string) => CATEGORY_NAME[category],
    },
    {
      title: i18n.t('cmp:running cluster'),
      value: 'cluster',
    },
    {
      title: i18n.t('running environment'),
      value: 'workspace',
      render: (workspace: string) => ENV_NAME[workspace],
    },
    {
      title: i18n.t('cmp:specifications'),
      value: 'plan',
      render: (plan: string) => PLAN_NAME[plan],
    },
    {
      title: i18n.t('cmp:number of references'),
      value: 'referenceInfos',
      render: (referenceInfos: any[] = []) => referenceInfos.length,
    },
    {
      title: i18n.t('create time'),
      value: 'createdAt',
      render: (createdAt: string) => moment(createdAt).format('YYYY-MM-DD HH:mm:ss'),
    },
  ];

  if (addonDetail.isOperator) {
    itemConfigs.push({
      title: i18n.t('status'),
      value: 'addonStatus',
      render: (addonStatus = 'Unknown') => <>{addonStatusMap[addonStatus]}</>,
    });
  }

  const jsonStr = addonDetail.config === null ? '' : JSON.stringify(addonDetail.config, null, 2);

  return (
    <Spin spinning={loading}>
      <div className="addon-detail-page">
        <div className="base-info mb-8">
          <span className="title font-medium">{i18n.t('cmp:basic info')}</span>
          <div className="info-grid">
            {map(itemConfigs, ({ title, value, render }) => (
              <div key={title}>
                <div className="param-k nowrap">{title}</div>
                <div className="param-v nowrap">
                  {render && isFunction(render) ? render(value ? addonDetail[value] : addonDetail) : addonDetail[value]}
                </div>
              </div>
            ))}
          </div>
        </div>
        {extra}
        <div className="ref mb-8">
          <span className="title font-medium">{i18n.t('cmp:reference detail')}</span>
          <Table
            columns={refTableList}
            dataSource={addonDetail.referenceInfos}
            pagination={false}
            rowKey="applicationName"
            scroll={{ x: '100%' }}
          />
        </div>
        <div className="config">
          <div className="flex justify-between items-center">
            <span className="title font-medium">{i18n.t('cmp:basic parameters')}</span>
            {!isEmpty(addonDetail.config) && (
              <span className="copy-all cursor-pointer cursor-copy">
                {i18n.t('copy all')}
                <Copy selector=".cursor-copy" opts={{ text: () => jsonStr }} />
              </span>
            )}
          </div>
          {map(addonDetail.config, (v, k) => (
            <div key={k}>
              <div className="param-k nowrap">{k}</div>
              <IF check={v}>
                <div className="param-v nowrap">{v}</div>
                <IF.ELSE />
                <div className="param-v nowrap">***</div>
              </IF>
            </div>
          ))}
        </div>
      </div>
    </Spin>
  );
};

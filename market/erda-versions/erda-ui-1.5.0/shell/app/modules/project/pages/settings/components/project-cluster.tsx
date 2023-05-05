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
import { Table, Tooltip } from 'antd';
import { DOC_PROJECT_RESOURCE_MANAGE, WORKSPACE_LIST } from 'common/constants';
import { SectionInfoEdit } from 'project/common/components/section-info-edit';
import i18n from 'i18n';
import { ErdaIcon } from 'common';
import projectStore from 'project/stores/project';
import clusterStore from 'app/modules/cmp/stores/cluster';

import './project-cluster.scss';

interface IProps {
  hasEditAuth: boolean;
}

const workSpaceMap = {
  DEV: i18n.t('dev environment'),
  TEST: i18n.t('test environment'),
  STAGING: i18n.t('staging environment'),
  PROD: i18n.t('prod environment'),
};

const renderBar = (type: string, record: PROJECT.ICluster, unit: string) => {
  let data;

  if (type === 'cpu') {
    data = {
      request: +(record?.cpuRequest || 0).toFixed(3),
      requestRate: +(record?.cpuRequestRate || 0).toFixed(3),
      requestByService: +(record?.cpuRequestByService || 0).toFixed(3),
      requestByServiceRate: +(record?.cpuRequestByServiceRate || 0).toFixed(3),
      requestByAddon: +(record?.cpuRequestByAddon || 0).toFixed(3),
      requestByAddonRate: +(record?.cpuRequestByAddonRate || 0).toFixed(3),
      quota: +(record?.cpuQuota || 0).toFixed(3),
      tips: record.cpuAvailable < record.cpuQuota ? record.tips : '',
    };
  } else {
    data = {
      request: +(record?.memRequest || 0).toFixed(3),
      requestRate: +(record?.memRequestRate || 0).toFixed(3),
      requestByService: +(record?.memRequestByService || 0).toFixed(3),
      requestByServiceRate: +(record?.memRequestByServiceRate || 0).toFixed(3),
      requestByAddon: +(record?.memRequestByAddon || 0).toFixed(3),
      requestByAddonRate: +(record?.memRequestByAddonRate || 0).toFixed(3),
      quota: +(record?.memQuota || 0).toFixed(3),
      tips: record.memAvailable < record.memQuota ? record.tips : '',
    };
  }

  const {
    request = 0,
    requestRate = 0,
    requestByService = 0,
    requestByServiceRate = 0,
    requestByAddon = 0,
    requestByAddonRate = 0,
    quota = 0,
    tips = '',
  } = data;

  const isOveruse = requestRate < 100;
  return (
    <div className={`quota-container ${isOveruse ? 'overuse' : ''}`}>
      <div className={'quota-bar'}>
        <Tooltip title={`${i18n.t('cmp:application used')} ${requestByService}${unit} (${requestByServiceRate}%)`}>
          <div
            className={`nowrap ${requestByServiceRate !== 0 ? 'border-right-color' : ''}`}
            style={{ width: `${requestByServiceRate}%` }}
          >
            {i18n.t('application')}
          </div>
        </Tooltip>
        <Tooltip title={`${i18n.t('cmp:addon used')} ${requestByAddon}${unit} (${requestByAddonRate}%)`}>
          <div
            className={`nowrap ${requestByAddonRate !== 0 ? 'border-right-color' : ''}`}
            style={{ width: `${requestByAddonRate}%` }}
          >
            addon
          </div>
        </Tooltip>
        <Tooltip
          title={`
              ${i18n.t('msp:available')}
              ${+(quota - request).toFixed(3)}${unit}
              (${+(100 - requestRate).toFixed(3)})%
            `}
        >
          <div className="nowrap" style={{ width: `${100 - requestRate}%` }}>
            {i18n.t('msp:available')}
          </div>
        </Tooltip>
      </div>
      {tips && (
        <Tooltip title={tips}>
          <ErdaIcon fill="danger-red" type="help" size="16" className="ml-1" />
        </Tooltip>
      )}
    </div>
  );
};

const ProjectCluster = ({ hasEditAuth }: IProps) => {
  const clusterList = clusterStore.useStore((s) => s.list);
  const { getClusterList } = clusterStore.effects;
  const info = projectStore.useStore((s) => s.info);
  const { updateProject } = projectStore.effects;

  React.useEffect(() => {
    hasEditAuth && getClusterList();
  }, [getClusterList, hasEditAuth]);
  const { resourceConfig } = info;

  const options: object[] = [];
  clusterList.forEach((item) => {
    options.push({
      value: item.name,
      name: item.name,
    });
  });

  const configData = {};
  const tableData: object[] = [];
  const fieldsList: object[] = [];
  const sortBy = WORKSPACE_LIST;
  resourceConfig &&
    sortBy.forEach((workspace) => {
      const name = workspace.toUpperCase();
      const cluster = resourceConfig?.[workspace];

      tableData.push({ workspace, ...cluster });
      configData[`${name}`] = cluster;
    });

  const readonlyForm = (
    <Table
      rowKey="workspace"
      dataSource={tableData}
      tableLayout="auto"
      columns={[
        {
          key: 'workspace',
          title: i18n.t('dop:environments'),
          width: 120,
          dataIndex: 'workspace',
          render: (val: string) => workSpaceMap[val] || val,
        },
        {
          title: i18n.t('dop:using clusters'),
          dataIndex: 'clusterName',
          align: 'left',
          width: 180,
        },
        {
          title: `CPU ${i18n.t('allocated and utilization rate')}`,
          width: 300,
          dataIndex: 'cpuQuota',
          align: 'center',
          render: (text: string, record: PROJECT.ICluster) => (
            <div className="flex items-center">
              <span className="text-left" style={{ width: 60 }}>
                {text ? `${+(+text)}${i18n.t('core')}` : `0${i18n.t('core')}`}
              </span>
              <span className="flex-grow ml-2">{renderBar('cpu', record, i18n.t('core'))}</span>
            </div>
          ),
        },
        {
          title: `MEM ${i18n.t('allocated and utilization rate')}`,
          width: 300,
          dataIndex: 'memQuota',
          align: 'center',
          render: (text: string, record: PROJECT.ICluster) => (
            <div className="flex items-center">
              <span className="text-left" style={{ width: 60 }}>
                {text ? `${+(+text)}GB` : '0GB'}
              </span>
              <span className="flex-grow ml-2">{renderBar('mem', record, 'GB')}</span>
            </div>
          ),
        },
      ]}
      pagination={false}
    />
  );
  return (
    <SectionInfoEdit
      hasAuth={hasEditAuth}
      data={{ clusterConfig: configData }}
      readonlyForm={readonlyForm}
      fieldsList={fieldsList}
      updateInfo={updateProject}
      name={i18n.t('dop:project quota')}
      desc={
        <span>
          {i18n.t(
            'For cluster resource information corresponding to each environment, the concept and settings of specific clusters, please see',
          )}
          <a href={DOC_PROJECT_RESOURCE_MANAGE} target="_blank" rel="noopener noreferrer">
            {' '}
            {i18n.t('documentation')}{' '}
          </a>
        </span>
      }
      formName={i18n.t('dop:cluster used by the environment')}
    />
  );
};

export default ProjectCluster;

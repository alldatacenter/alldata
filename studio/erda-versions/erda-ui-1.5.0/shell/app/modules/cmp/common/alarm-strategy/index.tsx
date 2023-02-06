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
import { map } from 'lodash';
import moment from 'moment';
import { useMount, useUnmount } from 'react-use';
import { Modal, Button, Spin, Tooltip } from 'antd';
import { Badge } from 'common';
import { goTo } from 'common/utils';
import { ColumnProps } from 'app/interface/common';
import i18n from 'i18n';
import { useLoading } from 'core/stores/loading';
import notifyGroupStore from 'application/stores/notify-group';
import orgMemberStore from 'common/stores/org-member';
import projectMemberStore from 'common/stores/project-member';
import cmpAlarmStrategyStore from 'app/modules/cmp/stores/alarm-strategy';
import mspAlarmStrategyStore from 'app/modules/msp/alarm-manage/alarm-strategy/stores/alarm-strategy';
import Table from 'common/components/table';
import { IActions } from 'common/components/table/interface';
import './index.scss';

const { confirm } = Modal;

enum ScopeType {
  ORG = 'org',
  PROJECT = 'project',
  MSP = 'msp',
}

const alarmStrategyStoreMap = {
  [ScopeType.ORG]: cmpAlarmStrategyStore,
  [ScopeType.MSP]: mspAlarmStrategyStore,
};

const memberStoreMap = {
  [ScopeType.ORG]: orgMemberStore,
  [ScopeType.MSP]: projectMemberStore,
};

interface IProps {
  scopeType: ScopeType.ORG | ScopeType.MSP;
  scopeId: string;
  commonPayload?: Obj;
}

const AlarmStrategyList = ({ scopeType, scopeId, commonPayload }: IProps) => {
  const memberStore = memberStoreMap[scopeType];
  const { getRoleMap } = memberStore.effects;
  const alarmStrategyStore = alarmStrategyStoreMap[scopeType];
  const [alertList, alarmPaging] = alarmStrategyStore.useStore((s) => [s.alertList, s.alarmPaging]);
  const { total, pageNo, pageSize } = alarmPaging;
  const [getAlertDetailLoading, getAlertsLoading, toggleAlertLoading] = useLoading(alarmStrategyStore, [
    'getAlertDetail',
    'getAlerts',
    'toggleAlert',
  ]);
  const { getAlerts, toggleAlert, deleteAlert, getAlarmScopes, getAlertTypes, getAlertTriggerConditions } =
    alarmStrategyStore.effects;
  const { clearAlerts } = alarmStrategyStore.reducers;
  const { getNotifyGroups } = notifyGroupStore.effects;

  useMount(() => {
    let payload = { scopeType, scopeId };
    if (scopeType === ScopeType.MSP) {
      payload = {
        scopeType: commonPayload?.scopeType,
        scopeId: commonPayload?.scopeId,
      };
    }
    getAlerts();
    getAlarmScopes();
    getAlertTypes();
    getNotifyGroups(payload);
    getRoleMap({ scopeType, scopeId: scopeType === ScopeType.MSP ? commonPayload?.scopeId : scopeId });
    getAlertTriggerConditions(scopeType);
  });

  useUnmount(() => {
    clearAlerts();
  });

  const handleDeleteAlarm = (id: number) => {
    confirm({
      title: i18n.t('dop:are you sure you want to delete this item?'),
      content: i18n.t('dop:the alarm strategy will be permanently deleted'),
      onOk() {
        deleteAlert(id);
      },
    });
  };

  const handlePageChange = (paging: { current: number; pageSize?: number }) => {
    const { current, pageSize: size } = paging;
    getAlerts({ pageNo: current, pageSize: size });
  };

  const alertListColumns: Array<ColumnProps<COMMON_STRATEGY_NOTIFY.IAlert>> = [
    {
      title: i18n.t('cmp:alarm name'),
      dataIndex: 'name',
    },
    {
      title: i18n.t('status'),
      dataIndex: 'enable',
      render: (enable) => (
        <Badge text={enable ? i18n.t('enable') : i18n.t('unable')} status={enable ? 'success' : 'default'} />
      ),
    },
    // ...insertWhen(scopeType === ScopeType.ORG, [
    //   {
    //     title: i18n.t('cmp:cluster'),
    //     dataIndex: 'clusterNames',
    //     width: 200,
    //     render: (clusterNames: string[]) => map(clusterNames, (clusterName) => alarmScopeMap[clusterName]).join(),
    //   },
    // ]),
    // ...insertWhen(scopeType === ScopeType.MSP && commonPayload?.projectType !== 'MSP', [
    //   {
    //     title: i18n.t('application'),
    //     dataIndex: 'appIds',
    //     width: 200,
    //     render: (appIds: string[]) => map(appIds, (appId) => alarmScopeMap[appId]).join(),
    //   },
    // ]),
    {
      title: i18n.t('default:notification target'),
      dataIndex: 'notifies',
      className: 'notify-info',
      ellipsis: true,
      render: (notifies: COMMON_STRATEGY_NOTIFY.INotifyGroupNotify[]) => {
        const tips = i18n.t('cmp:Notification group does not exist or has been remove. Please change one.');
        if (notifies?.length && notifies[0].notifyGroup?.name) {
          const groupNames = map(notifies, (item) => item.notifyGroup?.name).join(', ');
          const groupLength = notifies.length;
          return `${groupNames} ${i18n.t('cmp:and {length} others', { length: groupLength })}`;
        }
        return (
          <div className="flex-div flex">
            <Tooltip title={tips}>
              <span className="text-sub">{tips}</span>
            </Tooltip>
          </div>
        );
      },
    },
    {
      title: i18n.t('default:create time'),
      dataIndex: 'createTime',
      render: (text) => moment(text).format('YYYY-MM-DD HH:mm:ss'),
    },
  ];

  const actions: IActions<COMMON_STRATEGY_NOTIFY.IAlert> = {
    render: (record: COMMON_STRATEGY_NOTIFY.IAlert) => renderMenu(record),
  };

  const renderMenu = (record: COMMON_STRATEGY_NOTIFY.IAlert) => {
    const { editStrategy, deleteStrategy, enableStrategy } = {
      editStrategy: {
        title: i18n.t('edit'),
        onClick: () => {
          goTo(`./edit-strategy/${record.id}`);
        },
      },
      deleteStrategy: {
        title: i18n.t('delete'),
        onClick: () => {
          handleDeleteAlarm(record.id);
        },
      },
      enableStrategy: {
        title: record?.enable ? i18n.t('unable') : i18n.t('enable'),
        onClick: () => {
          toggleAlert({
            id: record.id,
            enable: !record.enable,
          }).then(() => {
            getAlerts({ pageNo });
          });
        },
      },
    };

    return [editStrategy, deleteStrategy, enableStrategy];
  };
  return (
    <div className="alarm-strategy">
      <div className="top-button-group">
        <Button
          type="primary"
          onClick={() => {
            goTo('./add-strategy');
          }}
        >
          {i18n.t('cmp:new strategy')}
        </Button>
      </div>
      <Spin spinning={getAlertsLoading || toggleAlertLoading}>
        <Table
          rowKey="id"
          columns={alertListColumns}
          dataSource={alertList}
          actions={actions}
          pagination={{
            current: pageNo,
            pageSize,
            total,
          }}
          onChange={handlePageChange}
        />
      </Spin>
    </div>
  );
};

export default AlarmStrategyList;

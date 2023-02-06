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
import { Table, Popconfirm, Tooltip } from 'antd';
import { ColumnProps } from 'core/common/interface';
import i18n from 'i18n';
import { contractStatueMap } from 'apiManagePlatform/pages/access-manage/components/config';
import apiAccessStore from 'apiManagePlatform/stores/api-access';
import { TableActions, Icon as CustomIcon, Ellipsis } from 'common';
import { useUpdate } from 'common/use-hooks';
import DetailModal from './detail-modal';
import { useLoading } from 'core/stores/loading';
import UpdateSLA from 'apiManagePlatform/components/update-sla';
import TrafficAuditDrawer from 'apiManagePlatform/components/traffic-audit-drawer';
import { isEmpty } from 'lodash';

interface IState {
  visible: boolean;
  trafficAuditVisible: boolean;
  selectedClient: API_ACCESS.Client;
  SLAVisible: boolean;
  selectContract: API_CLIENT.Contract;
}

const AuthorizationUser = ({ swaggerVersion, assetID }: { swaggerVersion: string; assetID: string }) => {
  const [clientList, clientPaging, slaList, accessDetail] = apiAccessStore.useStore((s) => [
    s.clientList,
    s.clientPaging,
    s.slaList,
    s.accessDetail.access,
  ]);
  const { deleteContracts, updateContracts, getClient, getSlaList } = apiAccessStore.effects;
  const [isFetch, isUpdateSLA] = useLoading(apiAccessStore, ['getClient', 'updateContracts']);
  const [state, updater, update] = useUpdate<IState>({
    visible: false,
    selectedClient: {},
    SLAVisible: false,
    selectContract: {},
    trafficAuditVisible: false,
  });
  const showModal = (record: API_ACCESS.Client) => {
    update({
      visible: true,
      selectedClient: record,
    });
  };
  const closeModal = () => {
    update({
      visible: false,
      selectedClient: {},
    });
  };
  const handlePagingChange = (pageNo: number) => {
    getClient({ swaggerVersion, assetID, pageNo, paging: true });
  };
  const handleProve = async (status: API_ACCESS.ContractStatue | 'delete', record: API_CLIENT.Contract) => {
    const { id, clientID, requestSLAID } = record;
    if (status === 'delete') {
      await deleteContracts({ contractID: id, clientID });
      getSlaList({ assetID, swaggerVersion });
    } else if (status === 'proved') {
      await updateContracts({ contractID: id, clientID, status });
      // 审批通过后若存在请求SLA，之前展示变更SLA弹框
      if (requestSLAID && status === 'proved') {
        handleUpdateSLA(record);
      }
    } else {
      await updateContracts({ contractID: id, clientID, status });
    }
    getClient({ swaggerVersion, assetID, pageNo: status === 'delete' ? 1 : clientPaging.pageNo, paging: true });
  };
  const handleUpdateSLA = (data?: API_CLIENT.Contract) => {
    update({
      selectContract: isEmpty(data) ? {} : data,
      SLAVisible: !isEmpty(data),
    });
  };
  const handleTrafficAudit = (data?: API_CLIENT.Contract) => {
    update({
      selectContract: isEmpty(data) ? {} : data,
      trafficAuditVisible: !isEmpty(data),
    });
  };
  const handleSubmitSLA = async (curSLAID: number) => {
    await updateContracts({ contractID: state.selectContract.id, clientID: state.selectContract.clientID, curSLAID });
    getClient({ swaggerVersion, assetID, pageNo: clientPaging.pageNo, paging: true });
    getSlaList({ assetID, swaggerVersion });
    updater.SLAVisible(false);
  };
  const columns: Array<ColumnProps<API_ACCESS.Client>> = [
    {
      title: i18n.t('client name'),
      dataIndex: ['client', 'displayName'],
      width: 120,
      render: (text, record) => {
        return (
          <div className="flex items-center justify-start">
            <div className="client_displayName">
              <Ellipsis title={text || record.client?.name} />
            </div>
            {record.contract.status === 'proved' && (
              <Tooltip title={i18n.t('traffic audit')}>
                <CustomIcon
                  className="ml-2 text-primary hover-active font-bold"
                  type="monitor"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleTrafficAudit(record.contract);
                  }}
                />
              </Tooltip>
            )}
          </div>
        );
      },
    },
    {
      title: i18n.t('client identifier'),
      dataIndex: ['client', 'name'],
      width: 120,
    },
    {
      title: i18n.t('current SLA'),
      dataIndex: ['contract', 'curSLAName'],
    },
    {
      title: i18n.t('applying SLA'),
      dataIndex: ['contract', 'requestSLAName'],
    },
    {
      title: i18n.t('status'),
      dataIndex: ['contract', 'status'],
      width: 80,
      render: (text, { contract }) => {
        if (contract.requestSLAID && text === 'proved') {
          return i18n.t('apply to change SLA');
        }
        return contractStatueMap[text]?.name;
      },
    },
    {
      title: i18n.t('operation'),
      dataIndex: 'permission',
      width: 240,
      fixed: 'right',
      render: ({ edit }: API_ACCESS.ClientPermission, { contract }) => {
        if (!edit) {
          return null;
        }
        const { status } = contract;
        return (
          <TableActions>
            {status !== 'disproved' && (
              <span
                className="table-operations-btn"
                onClick={() => {
                  handleUpdateSLA(contract);
                }}
              >
                {i18n.t('replace SLA')}
              </span>
            )}
            {(contractStatueMap[status].actions || []).map((item) => {
              return (
                <Popconfirm
                  key={item.value}
                  title={i18n.t('confirm to {action}', { action: i18n.t(item.action) })}
                  onConfirm={() => {
                    handleProve(item.value, contract);
                  }}
                >
                  <span>{item.name}</span>
                </Popconfirm>
              );
            })}
          </TableActions>
        );
      },
    },
  ];
  return (
    <>
      <Table
        rowKey={(record) => record.client.clientID}
        columns={columns}
        dataSource={clientList}
        loading={isFetch}
        pagination={{
          ...clientPaging,
          onChange: handlePagingChange,
        }}
        onRow={(record: API_ACCESS.Client) => {
          return {
            onClick: () => {
              showModal(record);
            },
          };
        }}
        scroll={{ x: 800 }}
      />
      <DetailModal
        visible={state.visible}
        dataSource={state.selectedClient as API_ACCESS.Client}
        onCancel={closeModal}
      />
      <UpdateSLA
        visible={state.SLAVisible}
        slaList={slaList}
        metaData={{
          currentSLAID: state.selectContract.curSLAID,
          defaultSLAID:
            state.selectContract.requestSLAID || state.selectContract.requestSLAID === 0
              ? state.selectContract.requestSLAID
              : state.selectContract.curSLAID,
        }}
        onCancel={handleUpdateSLA}
        onOk={handleSubmitSLA}
        confirmLoading={isUpdateSLA}
      />
      <TrafficAuditDrawer
        visible={state.trafficAuditVisible}
        onClose={handleTrafficAudit}
        queries={{
          client: state.selectContract.clientName,
          endpoint: accessDetail.endpointName,
          workspace: accessDetail.workspace?.toLowerCase(),
          projectID: accessDetail.projectID,
        }}
      />
    </>
  );
};
export default AuthorizationUser;

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
import { Spin, Table, Tabs, Tooltip } from 'antd';
import apiClientStore from 'apiManagePlatform/stores/api-client';
import routeInfoStore from 'core/stores/route';
import { Copy, DetailsPanel, Icon as CustomIcon, TableActions, Ellipsis } from 'common';
import { useUpdate } from 'common/use-hooks';
import i18n from 'i18n';
import { get, map } from 'lodash';
import { contractStatueMap } from 'apiManagePlatform/pages/client/components/config';
import { ColumnProps, PaginationProps } from 'core/common/interface';
import apiAccessStore from 'apiManagePlatform/stores/api-access';
import UpdateSLA from 'apiManagePlatform/components/update-sla';
import { useLoading } from 'core/stores/loading';
import TrafficAuditDrawer from 'apiManagePlatform/components/traffic-audit-drawer';
import './index.scss';

const { TabPane } = Tabs;
const defaultStatue = 'proved';

interface IState {
  showSecret: boolean;
  SLAVisible: boolean;
  trafficAuditVisible: boolean;
  selectRecord: API_CLIENT.Contract;
}

const ClientDetail = () => {
  const [{ showSecret, SLAVisible, selectRecord, trafficAuditVisible }, updater, update] = useUpdate<IState>({
    showSecret: false,
    SLAVisible: false,
    trafficAuditVisible: false,
    selectRecord: {},
  });
  const [
    clientDetail,
    disprovedContractList,
    disprovedContractPaging,
    provedContractList,
    provedContractPaging,
    provingContractList,
    provingContractPaging,
    unprovedContractList,
    unprovedContractPaging,
  ] = apiClientStore.useStore((s) => [
    s.clientDetail,
    s.disprovedContractList,
    s.disprovedContractPaging,
    s.provedContractList,
    s.provedContractPaging,
    s.provingContractList,
    s.provingContractPaging,
    s.unprovedContractList,
    s.unprovedContractPaging,
  ]);
  const slaList = apiAccessStore.useStore((s) => s.slaList);
  const { getSlaList, updateContracts } = apiAccessStore.effects;
  const { clearSla } = apiAccessStore.reducers;
  const params = routeInfoStore.useStore((s) => s.params);
  const { getClientDetail, getContractList } = apiClientStore.effects;
  const { clearContractList, clearClientDetail } = apiClientStore.reducers;
  const [isUpdateSLA] = useLoading(apiAccessStore, ['updateContracts']);
  const [isFetchDetail, isFetchList] = useLoading(apiClientStore, ['getClientDetail', 'getContractList']);
  React.useEffect(() => {
    if (params.id) {
      getClientDetail({ clientID: +params.id }).then((res) => {
        getContractList({ status: defaultStatue, paging: true, pageNo: 1, clientID: res.client.id });
      });
    }
    return () => {
      clearContractList();
      clearClientDetail();
      clearSla();
    };
  }, [clearClientDetail, clearContractList, clearSla, getClientDetail, getContractList, params.id]);
  const handleChangeTable = (status: API_CLIENT.ContractStatue, { current, pageSize }: PaginationProps) => {
    getContractList({ status, paging: true, pageNo: current, pageSize, clientID: clientDetail.client.id });
  };
  const handleChangeTab = (activeKey: API_CLIENT.ContractStatue) => {
    if (!dataMap[activeKey].list.length) {
      getContractList({ status: activeKey, paging: true, pageNo: 1, clientID: clientDetail.client.id });
    }
  };
  const handleUpdateSLA = (record: API_CLIENT.Contract) => {
    getSlaList({ assetID: record.assetID, swaggerVersion: record.swaggerVersion });
    update({
      SLAVisible: true,
      selectRecord: record,
    });
  };
  const handleCloseSLA = () => {
    update({
      SLAVisible: false,
      selectRecord: {},
    });
  };
  const handleSubmitSLA = async (requestSLAID: number) => {
    const { status, clientID, id } = selectRecord as API_CLIENT.Contract;
    await updateContracts({ requestSLAID, clientID, contractID: id });
    getContractList({ status, paging: true, pageNo: dataMap[status].paging.pageNo, clientID: clientDetail.client.id });
    handleCloseSLA();
  };
  const dataMap = {
    proved: {
      list: provedContractList,
      paging: provedContractPaging,
    },
    proving: {
      list: provingContractList,
      paging: provingContractPaging,
    },
    disproved: {
      list: disprovedContractList,
      paging: disprovedContractPaging,
    },
    unproved: {
      list: unprovedContractList,
      paging: unprovedContractPaging,
    },
  };
  const fields = [
    {
      label: i18n.t('client name'),
      value: get(clientDetail, ['client', 'displayName']) || get(clientDetail, ['client', 'name']),
    },
    {
      label: i18n.t('client identifier'),
      value: get(clientDetail, ['client', 'name']),
    },
    {
      label: i18n.t('description'),
      value: get(clientDetail, ['client', 'desc']),
    },
    {
      label: 'ClientID',
      value: (
        <span className="cursor-copy" data-clipboard-text={get(clientDetail, ['sk', 'clientID'])}>
          {get(clientDetail, ['sk', 'clientID'])}
        </span>
      ),
    },
    {
      label: 'ClientSecret',
      value: (
        <div className="flex justify-between items-start">
          {showSecret ? (
            <span className="cursor-copy" data-clipboard-text={get(clientDetail, ['sk', 'clientSecret'])}>
              {get(clientDetail, ['sk', 'clientSecret'])}
            </span>
          ) : (
            <span>******</span>
          )}
          <span
            className="hover-active ml-1"
            onClick={() => {
              updater.showSecret(!showSecret);
            }}
          >
            <CustomIcon type={showSecret ? 'openeye' : 'closeeye'} />
          </span>
        </div>
      ),
    },
  ];
  const getColumns = (statue: API_CLIENT.ContractStatue) => {
    const columns: Array<ColumnProps<API_CLIENT.Contract>> = [
      {
        title: i18n.t('API name'),
        dataIndex: 'assetName',
        width: 200,
        render: (text, record) => {
          return (
            <div className="flex items-center justify-start">
              <div className="asset_name">
                <Ellipsis title={text} />
              </div>
              {record.status === 'proved' && (
                <Tooltip title={i18n.t('traffic audit')}>
                  <CustomIcon
                    className="ml-2 text-primary hover-active font-bold"
                    type="monitor"
                    onClick={(e) => {
                      e.stopPropagation();
                      update({ trafficAuditVisible: true, selectRecord: record });
                    }}
                  />
                </Tooltip>
              )}
            </div>
          );
        },
      },
      {
        title: i18n.t('version'),
        dataIndex: 'swaggerVersion',
        width: 160,
      },
    ];
    if (statue === 'proved') {
      columns.push(
        ...[
          {
            title: i18n.t('current SLA'),
            dataIndex: 'curSLAName',
          },
          {
            title: i18n.t('request SLA'),
            dataIndex: 'requestSLAName',
          },
          {
            title: i18n.t('operation'),
            width: '150',
            dataIndex: 'id',
            fixed: 'right',
            render: (_id: number, record: API_CLIENT.Contract) => (
              <TableActions>
                <span
                  onClick={() => {
                    handleUpdateSLA(record);
                  }}
                >
                  {i18n.t('replace SLA')}
                </span>
              </TableActions>
            ),
          },
        ],
      );
    }
    return columns;
  };
  return (
    <Spin spinning={isFetchDetail}>
      <DetailsPanel
        baseInfoConf={{
          title: i18n.t('basic information'),
          panelProps: {
            fields,
          },
        }}
      />
      <Copy selector=".cursor-copy" />
      <div className="p-4 api-list">
        <div className="title text-base text-normal font-medium">{i18n.t('authorized API')}</div>
        <Tabs
          defaultActiveKey="proved"
          onChange={(v: string) => {
            handleChangeTab(v as API_CLIENT.ContractStatue);
          }}
        >
          {map(contractStatueMap, ({ name, value }) => (
            <TabPane key={value} tab={name}>
              <Table
                rowKey="id"
                loading={isFetchList}
                columns={getColumns(value)}
                dataSource={dataMap[value].list}
                pagination={dataMap[value].paging}
                onChange={(pagination) => {
                  handleChangeTable(value, pagination);
                }}
                scroll={{ x: 800 }}
              />
            </TabPane>
          ))}
        </Tabs>
      </div>
      <UpdateSLA
        visible={SLAVisible}
        slaList={slaList.filter((sla) => sla.source !== 'system')}
        metaData={{
          curSLAName: selectRecord.curSLAName,
          currentSLAID: selectRecord.curSLAID,
          defaultSLAID: selectRecord.requestSLAID,
          committedAt: selectRecord.slaCommittedAt,
        }}
        onCancel={handleCloseSLA}
        onOk={handleSubmitSLA}
        confirmLoading={isUpdateSLA}
      />
      <TrafficAuditDrawer
        visible={trafficAuditVisible}
        onClose={() => {
          updater.trafficAuditVisible(false);
        }}
        queries={{
          workspace: selectRecord.workspace?.toLowerCase(),
          endpoint: selectRecord.endpointName,
          client: selectRecord.clientName,
          projectID: selectRecord.projectID,
        }}
      />
    </Spin>
  );
};

export default ClientDetail;

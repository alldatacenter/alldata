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
import { TableActions, Ellipsis } from 'common';
import { useUpdate } from 'common/use-hooks';
import { Table, Popconfirm } from 'antd';
import { ColumnProps } from 'core/common/interface';
import i18n from 'i18n';
import apiAccessStore from 'apiManagePlatform/stores/api-access';
import { useLoading } from 'core/stores/loading';
import SlaEditor from 'apiManagePlatform/pages/access-manage/detail/sla-editor';
import { slaUnitMap, slaAuthorizationMap } from 'apiManagePlatform/pages/access-manage/components/config';
import { uniq } from 'lodash';
import './sla.scss';

interface IState {
  slaItem: API_ACCESS.SlaItem;
  visible: boolean;
}
const Sla = () => {
  const [state, , update] = useUpdate<IState>({
    visible: false,
    slaItem: {},
  });
  const [assetID, swaggerVersion, slaLis, canEdit, clientPaging] = apiAccessStore.useStore((s) => [
    s.accessDetail.access.assetID,
    s.accessDetail.access.swaggerVersion,
    s.slaList,
    s.accessDetail.permission.edit,
    s.clientPaging,
  ]);
  const { getSlaList, deleteSla, getClient } = apiAccessStore.effects;
  const [isLoading] = useLoading(apiAccessStore, ['getSlaList']);
  const getListOfSla = React.useCallback(() => {
    getSlaList({
      assetID,
      swaggerVersion,
    });
  }, [assetID, getSlaList, swaggerVersion]);
  React.useEffect(() => {
    if (assetID && swaggerVersion) {
      getListOfSla();
    }
  }, [assetID, getListOfSla, getSlaList, swaggerVersion]);

  const handleEditSla = (slaItem: API_ACCESS.SlaItem) => {
    update({
      visible: true,
      slaItem,
    });
  };

  const reloadClient = () => {
    getClient({ swaggerVersion, assetID, paging: true, pageNo: clientPaging.pageNo });
  };

  const handleDeleteSla = ({ id: slaID }: API_ACCESS.SlaItem) => {
    deleteSla({ assetID, swaggerVersion, slaID }).then(() => {
      getListOfSla();
      reloadClient();
    });
  };

  const closeEditModal = () => {
    update({
      visible: false,
      slaItem: {},
    });
  };

  const columns: Array<ColumnProps<API_ACCESS.SlaItem>> = [
    {
      title: i18n.t('SLA name'),
      dataIndex: 'name',
    },
    {
      title: i18n.t('request limit'),
      dataIndex: 'limits',
      width: 160,
      render: (limits: API_ACCESS.SlaLimit[]) => {
        const limitsStr = (limits || []).map(({ limit, unit }) => {
          return `${limit} ${slaUnitMap[unit]}`;
        });
        return <Ellipsis title={uniq(limitsStr).join(',')} />;
      },
    },
    {
      title: i18n.t('number of client'),
      dataIndex: 'clientCount',
      width: 160,
      render: (count) => count || 0,
    },
    {
      title: i18n.t('authorization method'),
      dataIndex: 'approval',
      width: 200,
      render: (approval) => slaAuthorizationMap[approval]?.name,
    },
    {
      title: i18n.t('operation'),
      dataIndex: 'id',
      width: 160,
      fixed: 'right',
      render: (_id, record: API_ACCESS.SlaItem) => {
        if (!canEdit || record.source === 'system') {
          return null;
        }
        return (
          <TableActions>
            <span
              onClick={() => {
                handleEditSla(record);
              }}
            >
              {i18n.t('edit')}
            </span>
            <Popconfirm
              title={i18n.t('confirm to {action}', { action: i18n.t('delete') })}
              onConfirm={() => {
                handleDeleteSla(record);
              }}
            >
              <span>{i18n.t('delete')}</span>
            </Popconfirm>
          </TableActions>
        );
      },
    },
  ];

  return (
    <>
      <Table
        loading={isLoading}
        rowKey="id"
        columns={columns}
        dataSource={slaLis}
        pagination={false}
        scroll={{ x: 800 }}
      />
      <SlaEditor
        mode="edit"
        visible={state.visible}
        dataSource={state.slaItem as API_ACCESS.SlaItem}
        onCancel={closeEditModal}
        afterEdit={reloadClient}
      />
    </>
  );
};
export default Sla;

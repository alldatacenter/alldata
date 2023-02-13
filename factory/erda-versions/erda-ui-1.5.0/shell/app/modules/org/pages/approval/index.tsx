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
import { Select, Tooltip } from 'antd';
import { CRUDTable } from 'common';
import { useUpdate } from 'common/use-hooks';
import { map, get, isEmpty } from 'lodash';
import { insertWhen } from 'common/utils';
import routeInfoStore from 'core/stores/route';
import { useLoading } from 'core/stores/loading';
import approvalStore from '../../stores/approval';
import moment from 'moment';
import { useUserMap } from 'core/stores/userMap';
import DetailModal from '../certificate/detail-modal';
import { typeMap as certificateTypeMap } from '../certificate/index';
import i18n from 'i18n';
import { ColumnProps } from 'core/common/interface';

const { Option } = Select;
const undoneStatusMap = {
  approved: {
    value: 'approved',
    name: i18n.t('passed'),
  },
  denied: {
    value: 'denied',
    name: i18n.t('rejected'),
  },
};

const typeMap = {
  certificate: {
    value: 'certificate',
    name: i18n.t('cmp:certificate reference'),
  },
  'lib-reference': {
    value: 'lib-reference',
    name: i18n.t('cmp:library reference'),
  },
  'unblock-application': {
    value: 'unblock-application',
    name: i18n.t('cmp:unblocked application'),
  },
};

enum statusMap {
  pending = 'pending',
  denied = 'denied',
  approved = 'approved',
}

const PureApproval = ({ type }: { type: APPROVAL.ApprovalType }) => {
  const userMap = useUserMap();
  const [loading] = useLoading(approvalStore, ['getApprovalList']);
  const [list, paging] = approvalStore.useStore((s) => {
    return type === 'done' ? [s.doneList, s.donePaging] : [s.undoneList, s.undonePaging];
  });
  const { getApprovalList, updateApproval } = approvalStore.effects;
  const { clearApprovalList } = approvalStore.reducers;
  const [{ status, chosenDetail }, updater] = useUpdate({
    status: undefined as string | undefined,
    chosenDetail: {} as { type: string; iosInfo: any; androidInfo: any },
  });

  const getColumns = ({ reloadList }: { reloadList: () => void }) => {
    const columns: Array<ColumnProps<APPROVAL.Item>> = [
      {
        title: i18n.t('name'),
        dataIndex: 'title',
        render: (v: string) => {
          return <Tooltip title={v}>{v}</Tooltip>;
        },
      },
      {
        title: i18n.t('cmp:apply for'),
        width: 140,
        dataIndex: 'desc',
      },
      {
        title: i18n.t('type'),
        dataIndex: 'type',
        width: 100,
        render: (val: APPROVAL.ApprovalItemType) => get(typeMap, `${val}.name`),
      },
      {
        title: i18n.t('cmp:submitter'),
        dataIndex: 'submitter',
        width: 120,
        render: (val: string) => {
          const curUser = userMap[val];
          return curUser ? curUser.nick || curUser.name : '';
        },
      },
      {
        title: i18n.t('cmp:submit time'),
        dataIndex: 'createdAt',
        width: 140,
        render: (val: string) => moment(val).format('YYYY-MM-DD HH:mm:ss'),
      },
      {
        title: i18n.t('operation'),
        dataIndex: 'op',
        width: type === 'done' ? 60 : 150,
        render: (_v: any, record: APPROVAL.Item) => {
          const { type: approvalType, status: approvalStatus, id, extra } = record;
          const { ios, android } = extra || {};
          let detail = {} as any;
          if (ios) {
            detail = {
              type: certificateTypeMap.IOS.value,
              iosInfo: JSON.parse(ios),
            };
          } else if (android) {
            detail = {
              type: certificateTypeMap.Android.value,
              androidInfo: JSON.parse(android),
            };
          }
          return (
            <div className="table-operations">
              {approvalType === 'certificate' && !isEmpty(detail) && (
                <span className="table-operations-btn" onClick={() => updater.chosenDetail(detail)}>
                  {i18n.t('download')}
                </span>
              )}
              {approvalStatus === statusMap.pending ? (
                <>
                  <span
                    className="table-operations-btn"
                    onClick={() => {
                      updateApproval({ id, status: statusMap.approved }).then(() => {
                        reloadList();
                      });
                    }}
                  >
                    {i18n.t('dop:approved')}
                  </span>
                  <span
                    className="table-operations-btn"
                    onClick={() => {
                      updateApproval({ id, status: statusMap.denied }).then(() => {
                        reloadList();
                      });
                    }}
                  >
                    {i18n.t('dop:denied')}
                  </span>
                </>
              ) : null}
            </div>
          );
        },
      },
    ];
    if (type === 'done') {
      columns.splice(
        4,
        0,
        ...[
          {
            title: i18n.t('cmp:approver'),
            dataIndex: 'approver',
            render: (val: string) => {
              const curUser = userMap[val];
              return curUser ? curUser.nick || curUser.name : '';
            },
          },
          {
            title: i18n.t('cmp:approval time'),
            dataIndex: 'approvalTime',
            width: 180,
            render: (val: string) => moment(val).format('YYYY-MM-DD HH:mm:ss'),
          },
          {
            title: i18n.t('cmp:approval result'),
            dataIndex: 'status',
            width: 100,
            render: (val: string) => get(undoneStatusMap, `${val}.name`),
          },
        ],
      );
    }
    return columns;
  };

  const filterConfig = React.useMemo(
    () => [
      ...insertWhen(type === 'done', [
        {
          type: Select,
          name: 'status',
          customProps: {
            placeholder: i18n.t('filter by status'),
            options: map(undoneStatusMap, ({ name, value }) => (
              <Option key={name} value={value}>
                {name}
              </Option>
            )),
            className: 'w-52',
            allowClear: true,
            onChange: (val: any) => updater.status(val),
          },
        },
      ]),
    ],
    [type, updater],
  );

  const extraQuery = React.useMemo(() => {
    const typeStatus = type === 'undone' ? statusMap.pending : '';
    return { type, status: status || typeStatus };
  }, [status, type]);

  return (
    <>
      <CRUDTable<APPROVAL.Item>
        key={type}
        isFetching={loading}
        getList={getApprovalList}
        clearList={() => clearApprovalList(type)}
        list={list}
        paging={paging}
        getColumns={getColumns}
        filterConfig={filterConfig}
        extraQuery={extraQuery}
      />
      <DetailModal detail={chosenDetail} onClose={() => updater.chosenDetail({})} />
    </>
  );
};

const Approval = () => {
  const type = routeInfoStore.getState((s) => s.params.approvalType) as APPROVAL.ApprovalType;
  return type ? <PureApproval type={type} key={type} /> : null;
};

export default Approval;

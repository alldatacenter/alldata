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
import { Tooltip } from 'antd';
import { Panel } from 'common';
import i18n from 'i18n';
import { useUserMap } from 'core/stores/userMap';
import moment from 'moment';

interface IProps {
  caseDetail: TREE.NODE;
}

const CaseInfo = (props: IProps) => {
  const { caseDetail } = props;
  const userMap = useUserMap();

  const fields = [
    {
      label: i18n.t('name'),
      valueKey: 'name',
      valueItem: ({ value: val }: any) => {
        return (
          <Tooltip title={val}>
            <div className="nowrap">{val}</div>
          </Tooltip>
        );
      },
    },
    {
      label: i18n.t('dop:commit message'),
      valueKey: 'desc',
      valueItem: ({ value: val }: any) => {
        return (
          <Tooltip title={val}>
            <div className="nowrap">{val}</div>
          </Tooltip>
        );
      },
    },
    // {
    //   label: i18n.t('creator'),
    //   valueKey: 'creatorID',
    //   valueItem: ({ value: val }:any) => {
    //     const curUser = userMap[val];
    //     return curUser ? (curUser.nick || curUser.name) : (val || '-');
    //   },
    // },
    // {
    //   label: i18n.t('create time'),
    //   valueKey: 'createdAt',
    //   valueItem: ({ value: val }:any) => {
    //     return val ? moment(val).format('YYYY-MM-DD HH:mm:ss') : '-';
    //   },
    // },
    {
      label: i18n.t('dop:updater'),
      valueKey: 'updaterID',
      valueItem: ({ value: val }: any) => {
        const curUser = userMap[val];
        return curUser ? curUser.nick || curUser.name : val || '-';
      },
    },
    {
      label: i18n.t('update time'),
      valueKey: 'updatedAt',
      valueItem: ({ value: val }: any) => {
        return val ? moment(val).format('YYYY-MM-DD HH:mm:ss') : '-';
      },
    },
  ];

  return <Panel fields={fields} data={caseDetail} />;
};

export default CaseInfo;

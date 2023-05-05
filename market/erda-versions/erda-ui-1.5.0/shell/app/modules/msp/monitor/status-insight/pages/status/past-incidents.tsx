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
import Table from 'common/components/table';
import moment from 'moment';
import { Link } from 'react-router-dom';
import { resolvePath } from 'common/utils';
import { ColumnProps } from 'core/common/interface';
import i18n from 'i18n';

interface IData {
  createAt?: string;
  durationFormat?: string;
  requestId?: string;
  lastUpdate?: string;
}

const PastIncidents = ({ pastIncidents, fetchData }: { pastIncidents: any[]; fetchData: () => void }) => {
  const dataSource = pastIncidents.map((item, k) => {
    return { ...item, key: k, createAt: moment(item.createAt / 1000000).format('YYYY-MM-DD HH:mm:ss') };
  });
  const pastIncidentsCols: Array<ColumnProps<IData>> = [
    {
      key: 'createAt',
      dataIndex: 'createAt',
      title: i18n.t('msp:downtime'),
    },
    {
      dataIndex: 'durationFormat',
      align: 'left',
      title: i18n.t('msp:duration'),
    },
    {
      dataIndex: 'requestId',
      align: 'left',
      title: i18n.t('msp:latest news'),
      render: (requestId: string, record: IData) => {
        if (record.lastUpdate) {
          return <span>{record.lastUpdate}</span>;
        } else if (record.requestId) {
          return (
            <Link to={resolvePath(`../../error/request-detail/${record.requestId}`)}>{i18n.t('msp:details')}</Link>
          );
        }
        return null;
      },
    },
  ];
  return (
    <div className="past-incidents">
      <Table rowKey="key" dataSource={dataSource} onChange={() => fetchData()} columns={pastIncidentsCols} />
    </div>
  );
};

export default PastIncidents;

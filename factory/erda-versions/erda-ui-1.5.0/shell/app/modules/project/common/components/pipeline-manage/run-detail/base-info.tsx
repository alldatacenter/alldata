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
import { Panel } from 'common';
import { PipelineStatus } from './config';
import { secondsToTime } from 'common/utils';
import moment from 'moment';
import i18n from 'i18n';

import './base-info.scss';

interface IProps {
  data: PIPELINE.IPipelineDetail;
}

const BaseInfo = (props: IProps) => {
  const { data } = props;
  const fields = [
    {
      label: `${i18n.t('pipeline')} ID`,
      valueKey: 'id',
    },
    {
      label: i18n.t('status'),
      valueKey: 'status',
      valueItem: ({ value: val }: any) => {
        const definedStatus = PipelineStatus.find((s) => s.status === val) || ({ colorClass: 'gray', msg: val } as any);
        if (definedStatus) {
          const { jumping, colorClass, msg } = definedStatus;
          const statusStyle = `flow-${colorClass} ${jumping ? 'jumping' : ''}`;
          return (
            <div className="pipeline-detail-status">
              <span className={`status-icon ${statusStyle}`} />
              <span>{msg}</span>
            </div>
          );
        }
        return val || '-';
      },
    },
    {
      label: i18n.t('duration'),
      valueKey: 'costTimeSec',
      valueItem: ({ value: val }: any) => {
        return val && val !== -1 ? `${i18n.t('dop:time cost')} ${secondsToTime(+val)}` : '-';
      },
    },
    {
      label: i18n.t('common:start at'),
      valueKey: 'timeBegin',
      valueItem: ({ value: val }: any) => (val ? moment(val).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      label: i18n.t('common:end at'),
      valueKey: 'timeEnd',
      valueItem: ({ value: val }: any) => (val ? moment(val).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
  ];

  return <Panel fields={fields} data={data} />;
};

export default BaseInfo;

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
import moment, { Moment } from 'moment';
import i18n from 'i18n';
import { Col, DatePicker, Row, message } from 'antd';
import testPlanStore from 'project/stores/test-plan';
import { useUserMap } from 'core/stores/userMap';
import { get, map } from 'lodash';

const BasicInfo = () => {
  const { updateDate } = testPlanStore.effects;
  const { testPlan } = testPlanStore.useStore((s) => s.planReport);
  const userMap = useUserMap();
  const data = React.useMemo(() => {
    const { startedAt, endedAt, ownerID, partnerIDs = [] } = testPlan || {};
    const startDate = startedAt ? moment(startedAt).format('YYYY-MM-DD') : undefined;
    const endDate = endedAt ? moment(endedAt).format('YYYY-MM-DD') : undefined;
    const ownerName = get(userMap, [ownerID, 'nick'], '');
    const partnerNames = map(partnerIDs, (partnerID) => get(userMap, [partnerID, 'nick'], ''));
    const partnerNameStr = partnerNames.join(' ');
    return { startDate, endDate, ownerName, partnerNames, partnerNameStr };
  }, [testPlan, userMap]);
  const basicInfo = [
    {
      label: i18n.t('dop:test leader'),
      text: data.ownerName,
    },
    {
      label: i18n.t('dop:start date'),
      text: data.startDate,
      dateKey: 'timestampSecStartedAt',
    },
    {
      label: i18n.t('dop:test participant'),
      text: data.partnerNameStr,
    },
    {
      label: i18n.t('dop:end date'),
      text: data.endDate,
      dateKey: 'timestampSecEndedAt',
    },
  ];

  const setDate = (dateKey: string, dateString: string, date: Moment) => {
    if (!dateString) {
      message.error(i18n.t('dop:date cannot be empty'));
      return;
    }
    if (
      (dateKey === 'timestampSecStartedAt' && data.endDate && dateString > data.endDate) ||
      (dateKey === 'timestampSecEndedAt' && data.startDate && dateString < data.startDate)
    ) {
      message.error(i18n.t('dop:the start date cannot be later than the end date'));
      return;
    }
    updateDate({ [dateKey]: Math.floor(date.valueOf() / 1000) }).then(() => {
      message.success(i18n.t('dop:update completed'));
    });
  };

  return (
    <Row>
      {basicInfo.map((info) => (
        <Col span={12} className="mt-4" key={info.label}>
          <div className="text-desc">{info.label}</div>
          <div className="mt-2 text-sm">
            {info.dateKey ? (
              <DatePicker
                allowClear={false}
                value={info.text ? moment(info.text) : undefined}
                onChange={(date, dateString) => setDate(info.dateKey, dateString, date as Moment)}
              />
            ) : (
              info.text || i18n.t('none')
            )}
          </div>
        </Col>
      ))}
    </Row>
  );
};

export default BasicInfo;

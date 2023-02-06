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
import { DatePicker, InputNumber, message } from 'antd';
import { FormModal } from 'common';
import { qs, setApiWithOrg } from 'common/utils';
import moment, { Moment } from 'moment';
import { FormInstance } from 'core/common/interface';
import i18n from 'i18n';

const DAY_RANGE = 7; // unit: d
const DEFAULT_INTERVAL_H = 1; // unit: h
const DEFAULT_INTERVAL_MS = DEFAULT_INTERVAL_H * 60 * 60 * 1000;

interface IProps {
  start: number;
  visible: boolean;
  query?: {
    [prop: string]: any;
  };
  onCancel: () => void;
}

const DownloadLogFormModal = ({ start, visible, query, onCancel }: IProps) => {
  const handleDownload = ({ startTime, endTime }: { startTime: Moment; endTime: number }) => {
    const { taskID, downloadAPI, fetchApi, end, stream, ...rest }: any = query;
    const requestQuery = { ...rest };
    requestQuery.stream = stream || 'stdout';
    requestQuery.start = startTime.valueOf() * 1000000;
    const now = moment().valueOf();
    const duration = startTime.valueOf() + endTime * 60 * 1000;
    requestQuery.end = Math.min(duration, now) * 1000000;
    const customRequestQuery = { ...requestQuery, count: 200, source: 'job', id: `pipeline-task-${taskID}` };
    const logFile = downloadAPI
      ? `${downloadAPI}?${qs.stringify(customRequestQuery)}`
      : `${fetchApi || '/api/runtime/logs'}/actions/download?${qs.stringify(requestQuery)}`;
    window.open(setApiWithOrg(logFile));
    onCancel();
  };

  const disabledStartDate = (startValue: Moment | undefined) => {
    if (!startValue) return false;
    return moment().subtract(DAY_RANGE + 1, 'days') > startValue || startValue > moment();
  };

  const fieldsList = [
    {
      name: 'startTime',
      label: i18n.t('common:start at'),
      required: true,
      getComp: ({ form }: { form: FormInstance }) => (
        <DatePicker
          className="w-full"
          disabledDate={disabledStartDate}
          showTime
          showToday={false}
          format="YYYY-MM-DD HH:mm:ss"
          placeholder={i18n.t('common:select log start time')}
          defaultPickerValue={moment(start / 1000000 - DEFAULT_INTERVAL_MS)}
          onOk={(value: Moment) => {
            form.setFieldsValue({ startTime: value });
          }}
        />
      ),
    },
    {
      name: 'endTime',
      label: i18n.t('common:duration(minutes)'),
      required: true,
      initialValue: 60,
      getComp: ({ form }: { form: FormInstance }) => (
        <InputNumber
          min={1}
          max={60}
          className="w-full"
          onChange={(duration) => {
            form.setFieldsValue({ endTime: duration });
          }}
          placeholder={i18n.t('common:please enter any time from 1 to 60 minutes')}
        />
      ),
    },
  ];

  return (
    <FormModal
      title={i18n.t('common:log download')}
      visible={visible}
      fieldsList={fieldsList}
      onOk={handleDownload}
      onCancel={() => {
        onCancel();
      }}
      modalProps={{ destroyOnClose: true }}
    />
  );
};

export { DownloadLogFormModal as DownloadLogModal };

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
import { FormModal, IFormItem } from 'common';
import { getTimeRanges } from 'common/utils';
import i18n from 'i18n';
import { DatePicker } from 'antd';
import moment, { Moment } from 'moment';
import dopStore from 'dop/stores';
import { AppSelector } from 'application/common/app-selector';

const { RangePicker } = DatePicker;

export interface IMetaData {
  projectId: number;
  projectName: string;
  appId?: number;
}

interface IProps {
  visible: boolean;
  metaData: IMetaData;
  onCancel: () => void;
  afterSubmit?: (res?: any) => void;
}

const ApplyUnblockModal = ({ visible, onCancel, afterSubmit, metaData }: IProps) => {
  const fieldsList: IFormItem[] = [
    {
      label: i18n.t('default:project name'),
      initialValue: metaData.projectName,
      name: 'targetName',
      itemProps: {
        disabled: true,
      },
    },
    {
      label: i18n.t('default:application'),
      type: 'custom',
      name: 'appIDs',
      getComp: () => <AppSelector projectId={`${metaData.projectId}`} mode="multiple" />,
    },
    {
      label: i18n.t('runtime:deployment time'),
      name: 'dataRange',
      getComp: () => (
        <RangePicker
          placeholder={[i18n.t('common:start at'), i18n.t('common:end at')]}
          showTime={{ format: 'HH:mm' }}
          format="YYYY-MM-DD HH:mm"
          disabledDate={(time) => !!time && time.valueOf() < moment().subtract(1, 'd').valueOf()}
          ranges={getTimeRanges()}
        />
      ),
      rules: [
        {
          validator: (_rule: any, [start, end]: [Moment, Moment], callback: Function) => {
            if (end.diff(start, 'days') > 7) {
              callback(i18n.t('dop:The deployment time should not be longer than 7 days.'));
              return;
            }
            callback();
          },
        },
      ],
    },
    {
      label: i18n.t('default:specific content'),
      type: 'textArea',
      name: 'desc',
      itemProps: {
        maxLength: 1024,
      },
    },
  ];
  const handleOk = (data: { targetName: string; appIDs: number[]; dataRange: Moment[]; desc: string }) => {
    const { dataRange, appIDs, desc, targetName } = data;
    const [start, end] = dataRange;
    const extra = {
      appIDs: appIDs.join(','),
      start: start.format(),
      end: end.format(),
    };
    const payload = { extra, type: 'unblock-application', desc, targetId: metaData.projectId, targetName };
    dopStore.applyUnblock(payload).then((res) => {
      onCancel();
      afterSubmit && afterSubmit(res);
    });
  };

  return (
    <FormModal
      title={i18n.t('dop:apply to deploy')}
      fieldsList={fieldsList}
      visible={visible}
      modalProps={{
        destroyOnClose: true,
      }}
      onOk={handleOk}
      onCancel={onCancel}
    />
  );
};

export default ApplyUnblockModal;

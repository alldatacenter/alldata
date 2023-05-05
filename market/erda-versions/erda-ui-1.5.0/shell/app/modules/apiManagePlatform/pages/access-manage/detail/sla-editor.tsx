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

import React, { MutableRefObject } from 'react';
import { FormModal, IFormItem } from 'common';
import i18n from 'i18n';
import { slaAuthorizationMap } from 'apiManagePlatform/pages/access-manage/components/config';
import Limit from 'apiManagePlatform/pages/access-manage/detail/limit';
import { map } from 'lodash';
import apiAccessStore from 'apiManagePlatform/stores/api-access';
import { Button } from 'antd';
import { FormInstance } from 'core/common/interface';
import { useLoading } from 'core/stores/loading';
import { regRules } from 'common/utils';

export type IData = Omit<API_ACCESS.UpdateSla, 'assetID' | 'swaggerVersion' | 'default'>;

interface IProps {
  dataSource?: API_ACCESS.SlaItem;
  mode: 'add' | 'edit';
  visible: boolean;
  onCancel: () => void;
  afterEdit?: () => void;
}

const SlaEditor = ({ visible, onCancel, mode, dataSource, afterEdit }: IProps) => {
  const formRef = React.useRef({}) as MutableRefObject<FormInstance>;
  const [assetID, swaggerVersion] = apiAccessStore.useStore((s) => [
    s.accessDetail.access.assetID,
    s.accessDetail.access.swaggerVersion,
  ]);
  const { updateSla, addSla, getSlaList } = apiAccessStore.effects;
  const loading = useLoading(apiAccessStore, ['addSla', 'updateSla']);

  const handleOk = () => {
    formRef.current.validateFields().then(async (data: any) => {
      if (mode === 'add') {
        await addSla({
          assetID,
          swaggerVersion,
          ...data,
        });
      } else {
        await updateSla({
          assetID,
          swaggerVersion,
          ...data,
        });
        afterEdit && afterEdit();
      }
      getSlaList({ assetID, swaggerVersion });
      onCancel();
    });
  };
  const fieldsList: IFormItem[] = [
    {
      name: 'slaID',
      initialValue: dataSource?.id,
      itemProps: {
        type: 'hidden',
      },
    },
    {
      label: i18n.t('SLA name'),
      name: 'name',
      initialValue: dataSource?.name,
      itemProps: {
        placeholder: i18n.t('please enter'),
        autoComplete: 'off',
        maxLength: 36,
      },
      rules: [regRules.lenRange(2, 36)],
    },
    {
      label: i18n.t('SLA description'),
      name: 'desc',
      type: 'textArea',
      required: false,
      initialValue: dataSource?.desc,
      itemProps: {
        placeholder: i18n.t('please enter'),
        autoComplete: 'off',
        maxLength: 191,
      },
    },
    {
      label: i18n.t('authorization method'),
      labelTip: i18n.t('there can only be one automatically authorized SLA'),
      name: 'approval',
      type: 'select',
      initialValue: dataSource?.approval,
      options: map(slaAuthorizationMap, ({ name, value }) => ({ name, value })),
      itemProps: {
        placeholder: i18n.t('please select'),
      },
    },
    {
      label: i18n.t('request limit'),
      name: 'limits',
      initialValue: dataSource?.limits || [],
      config: {
        valuePropType: 'array',
      },
      getComp(): React.ReactElement<any> | string {
        return <Limit mode="single" />;
      },
      rules: [
        {
          validator: (_rule: any, value: API_ACCESS.BaseSlaLimit[], callback: Function) => {
            let errMsg: any;
            (value || []).forEach(({ limit, unit }, index) => {
              if (!(limit && unit) && !errMsg) {
                errMsg = i18n.t('The {index} item quantity and unit cannot be empty.', { index: index + 1 });
              }
            });
            callback(errMsg);
          },
        },
      ],
    },
  ];
  const footer = (
    <>
      <Button key="back" onClick={onCancel}>
        {i18n.t('cancel')}
      </Button>
      <Button key="submit" type="primary" loading={loading.some((t) => t)} onClick={handleOk}>
        {i18n.t('ok')}
      </Button>
    </>
  );
  return (
    <FormModal
      title={i18n.t(`${mode} {name}`, { name: 'SLA' })}
      ref={formRef}
      visible={visible}
      onCancel={onCancel}
      fieldsList={fieldsList}
      modalProps={{
        destroyOnClose: true,
        footer,
      }}
    />
  );
};

export default SlaEditor;

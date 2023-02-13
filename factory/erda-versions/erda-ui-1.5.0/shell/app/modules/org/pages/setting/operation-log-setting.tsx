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

import { Form } from 'dop/pages/form-editor/index';
import i18n from 'i18n';
import { Button } from 'antd';
import auditStore from 'org/stores/audit';
import React from 'react';
import { useEffectOnce } from 'react-use';

export const OperationLogSetting = () => {
  const [disableButton, setDisableButton] = React.useState(true);
  const setting = auditStore.useStore((s) => s.setting);
  const { getAuditLogSetting } = auditStore.effects;
  const { clearAuditLogSetting } = auditStore.reducers;
  const form = React.useRef();
  useEffectOnce(() => {
    getAuditLogSetting().then(() => {
      setDisableButton(false);
    });
    return () => {
      clearAuditLogSetting();
    };
  });

  const fieldsList = [
    {
      label: i18n.t('cmp:Operation log retention days'),
      component: 'inputNumber',
      key: 'interval',
      componentProps: {
        min: 1,
        max: 720,
        step: 1,
        precision: 0,
        onChange(v: number) {
          setDisableButton(!v);
        },
      },
      required: true,
      type: 'inputNumber',
    },
  ];
  const onSubmit = (data: any) => {
    auditStore.effects.updateAuditLogSetting(data);
  };

  return (
    <Form formRef={form} fields={fieldsList} value={setting}>
      <Form.Submit
        Button={Button}
        type="primary"
        onSubmit={onSubmit}
        disabled={disableButton}
        text={i18n.t('update')}
      />
    </Form>
  );
};

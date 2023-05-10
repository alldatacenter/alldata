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
import { Button } from 'antd';
import { map } from 'lodash';
import { useUpdate } from 'common/use-hooks';
import { FormModal } from 'app/configForm/nusi-form/form-modal';
import i18n from 'i18n';

interface IProps {
  currentData: Obj;
  onSubmit: (arg: Obj) => void;
}

const AddScope = (props: IProps) => {
  const { onSubmit, currentData } = props;
  const [{ visible }, updater] = useUpdate({
    visible: false,
  });
  const currentScopeKey = Object.keys(currentData);
  const currentScopeName = map(currentData, 'name');

  const onOk = (v: Obj) => {
    onSubmit(v);
    onClose();
  };

  const onOpen = () => {
    updater.visible(true);
  };

  const onClose = () => {
    updater.visible(false);
  };

  const fields = [
    {
      label: 'scope key',
      required: true,
      component: 'input',
      key: 'key',
      rules: [
        {
          validator: (val: string) => {
            let tip = '';
            const keyReg = /^[a-zA-Z]+$/;
            if (currentScopeKey.includes(val)) tip = i18n.t('{name} already exists', { name: 'key' });
            if (!tip && !keyReg.test(val)) tip = i18n.t('key only can be letters');
            return [!tip, tip];
          },
        },
      ],
    },
    {
      label: i18n.t('name'),
      required: true,
      component: 'input',
      key: 'name',
      rules: [
        {
          validator: (val: string) => {
            let tip = '';
            if (currentScopeName.includes(val)) tip = i18n.t('{name} already exists', { name: i18n.t('name') });
            return [!tip, tip];
          },
        },
      ],
    },
  ];

  return (
    <>
      <Button type="primary" onClick={onOpen}>
        {i18n.t('add {name}', { name: 'scope' })}
      </Button>
      <FormModal
        width={650}
        visible={visible}
        title={i18n.t('add {name}', { name: 'scope' })}
        onOk={onOk}
        onCancel={onClose}
        fieldList={fields}
      />
    </>
  );
};

export default AddScope;

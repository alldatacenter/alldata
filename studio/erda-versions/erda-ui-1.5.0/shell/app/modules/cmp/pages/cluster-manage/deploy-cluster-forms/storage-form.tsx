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
import i18n from 'i18n';
import { IFormProps, regRulesMap, FormUnitContainer } from '../form-utils';
import { RenderPureForm, ReadonlyForm } from 'common';
import { isEmpty, get } from 'lodash';
import { Switch } from 'antd';

// 存储配置
export const StorageForm = ({ form, isReadonly, data, curRef }: IFormProps) => {
  const formPrefix = 'config.storage';
  const [isNas, setIsNas] = React.useState(true);
  React.useEffect(() => {
    const storage = get(data, formPrefix) || {};
    if (!storage.nas && !isEmpty(storage.gluster)) {
      // gluster
      setIsNas(false);
    } else {
      setIsNas(true);
    }
  }, [data]);
  let fieldsList = [
    {
      label: i18n.t('cmp:absolute path of mount point'),
      name: `${formPrefix}.mountPoint`,
      initialValue: '/netdata',
      rules: [{ ...regRulesMap.absolutePath }],
    },
    {
      getComp: () => (
        <Switch checkedChildren="nas" unCheckedChildren="gluster" checked={isNas} onClick={() => setIsNas(!isNas)} />
      ),
    },
  ];
  const nasFields = [
    {
      label: i18n.t('cmp:NAS mount address'),
      name: `${formPrefix}.nas`,
    },
  ];
  const glusterFields = [
    {
      label: i18n.t('version'),
      name: `${formPrefix}.gluster.version`,
      type: 'radioGroup',
      initialValue: '4.1',
      options: ['3.12', '4.0', '4.1', '5', '6'].map((v) => ({ value: v, name: v })),
    },
    {
      label: i18n.t('cmp:service IP list'),
      name: `${formPrefix}.gluster.hosts`,
      type: 'select',
      itemProps: {
        mode: 'tags',
        tokenSeparators: [';'],
        dropdownStyle: { display: 'none' },
        placeholder: i18n.t('cmp:Please enter the IP, separated by pressing Enter or semicolons, up to 5 allowed'),
      },
      options: [],
      rules: [
        {
          validator: (_rule: any, value: any, callback: Function) => {
            let pass = false;
            let errorMsg = i18n.t('cmp:Please fill in the correct IP, separated by semicolon.');
            if (value) {
              const replicaNum = form.getFieldValue(['config', 'storage', 'gluster', 'replica']);
              value.forEach((item: string) => {
                const o = item.replace(/\s+/g, '');
                o !== '' && (pass = regRulesMap.ip.pattern.test(o));
              });
              if (pass && value.length % replicaNum !== 0) {
                errorMsg = i18n.t('cmp:The number of IPs should be a multiple of the number of copies.');
                pass = false;
              }
            }
            return pass ? callback() : callback(errorMsg);
          },
        },
      ],
    },
    {
      label: i18n.t('cmp:number of copies'),
      name: `${formPrefix}.gluster.replica`,
      type: 'inputNumber',
      initialValue: 3,
      itemProps: { max: 10, min: 1 },
    },
    {
      label: i18n.t('cmp:whether to install the server'),
      name: `${formPrefix}.gluster.server`,
      type: 'switch',
      initialValue: true,
    },
    {
      label: i18n.t('cmp:storage path'),
      name: `${formPrefix}.gluster.brick`,
      initialValue: '/brick',
    },
  ];
  fieldsList = fieldsList.concat(isNas ? nasFields : (glusterFields as any));
  return (
    <FormUnitContainer title={i18n.t('cmp:storage configs')} curRef={curRef}>
      {isReadonly ? (
        <ReadonlyForm fieldsList={fieldsList} data={data} />
      ) : (
        <RenderPureForm list={fieldsList} form={form} layout="vertical" className="deploy-form-render" />
      )}
    </FormUnitContainer>
  );
};

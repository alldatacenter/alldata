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

// 集群配置信息
export const ClusterConfigForm = ({ form, isReadonly, data, curRef }: IFormProps) => {
  const formPrefix = 'config.cluster';
  const fieldsList = [
    {
      label: i18n.t('cluster name'),
      name: `${formPrefix}.name`,
      rules: [{ ...regRulesMap.clusterName }],
    },
    {
      label: i18n.t('type'),
      name: `${formPrefix}.type`,
      type: 'radioGroup',
      initialValue: 'k8s',
      options: ['k8s', 'dcos'].map((v) => ({ value: v, name: v })),
    },
    {
      label: i18n.t('cmp:DNS server'),
      name: `${formPrefix}.nameservers`,
      type: 'select',
      itemProps: {
        mode: 'tags',
        tokenSeparators: [';', ' '],
        dropdownStyle: { display: 'none' },
        placeholder: i18n.t('cmp:Please enter the IP, separated by pressing Enter or semicolons, up to 5 allowed'),
      },
      options: [],
      rules: [
        {
          validator: (_rule: any, value: any, callback: Function) => {
            let pass = false;
            if (value) {
              if (value.length > 5) return callback(i18n.t('cmp:fill in up to 5 IPs'));
              value.forEach((item: string) => {
                const o = item.replace(/\s+/g, '');
                o !== '' && (pass = regRulesMap.ip.pattern.test(o));
              });
            }
            return pass ? callback() : callback(i18n.t('cmp:Please fill in the correct IP, separated by semicolon.'));
          },
        },
      ],
    },
    {
      label: i18n.t('cmp:container segment'),
      name: `${formPrefix}.containerSubnet`,
      initialValue: '9.0.0.0/8',
      rules: [{ ...regRulesMap.subnet }],
    },
    {
      label: i18n.t('cmp:VIP segment'),
      name: `${formPrefix}.virtualSubnet`,
      initialValue: '10.96.0.0/12',
      rules: [{ ...regRulesMap.subnet }],
    },
    {
      label: i18n.t('cmp:whether to install offline'),
      name: `${formPrefix}.offline`,
      initialValue: false,
      required: false,
      type: 'switch',
      itemProps: { type: 'hidden' },
    },
  ];
  return (
    <FormUnitContainer title={i18n.t('cluster infos')} curRef={curRef}>
      {isReadonly ? (
        <ReadonlyForm fieldsList={fieldsList} data={data} />
      ) : (
        <RenderPureForm list={fieldsList} form={form} layout="vertical" className="deploy-form-render" />
      )}
    </FormUnitContainer>
  );
};

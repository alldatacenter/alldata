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
import { find } from 'lodash';
import { RenderPureForm, ReadonlyForm } from 'common';

// 平台配置
export const PlatformForm = ({ form, isReadonly, data, curRef }: IFormProps) => {
  const formPrefix = 'config.platform';
  let fieldsList = [
    {
      label: i18n.t('resource environment'),
      name: `${formPrefix}.environment`,
      initialValue: '',
      required: false,
      itemProps: { type: 'hidden' },
    },
    // {
    //   label: '分配组件域名',
    //   name: `${formPrefix}.assignDomains`,
    //   initialValue: {},
    //   required: false,
    //   itemProps: { type: 'hidden' },
    // },
    {
      label: i18n.t('cmp:allocation component node'),
      name: `${formPrefix}.assignNodes.nexus`,
      itemProps: {
        placeholder: i18n.t('nexus:'),
      },
      rules: [
        {
          validator: (_rule: any, value: any, callback: Function) => {
            let pass = true;
            let errorMsg = i18n.t('cmp:please input correct IP');
            if (!regRulesMap.ip.pattern.test(value)) {
              pass = false;
            } else {
              errorMsg = i18n.t('cmp:The allocated node should be a certain one in the node list. Please check.');
            }
            const nodes = form.getFieldValue(['config', 'nodes']);
            if (nodes && pass) {
              pass = !!find(nodes, (item: any) => item.ip === value);
            } else {
              pass = false;
            }
            return pass ? callback() : callback(errorMsg);
          },
        },
      ],
    },
    {
      label: i18n.t('cmp:extensive domain'),
      name: `${formPrefix}.wildcardDomain`,
      rules: [{ ...regRulesMap.wildcardDomain }],
    },
    {
      label: i18n.t('cmp:whether to use the mixed master node'),
      name: `${formPrefix}.acceptMaster`,
      initialValue: false,
      type: 'switch',
    },
    {
      label: i18n.t('cmp:whether to use mixed lb nodes'),
      name: `${formPrefix}.acceptLB`,
      initialValue: false,
      type: 'switch',
    },
    {
      label: i18n.t('cmp:absolute path for local data storage'),
      name: `${formPrefix}.dataRoot`,
      initialValue: '/data',
      rules: [{ ...regRulesMap.absolutePath }],
    },
    {
      label: i18n.t('cmp:lb access protocol'),
      name: `${formPrefix}.scheme`,
      type: 'radioGroup',
      initialValue: 'http',
      options: ['http', 'https'].map((v) => ({ value: v, name: v })),
      itemProps: {
        onChange: (e: any) => {
          const port = e.target.value === 'http' ? 80 : 443;
          form.setFieldsValue({ 'config.platform.port': port });
        },
      },
    },
    {
      label: i18n.t('cmp:lb access port '),
      name: `${formPrefix}.port`,
      initialValue: 80,
      rules: [{ ...regRulesMap.port }],
    },
    {
      label: i18n.t('cmp:image host'),
      name: `${formPrefix}.registryHost`,
    },
    {
      label: i18n.t('cmp:platform-vpn client network segment'),
      name: `${formPrefix}.openvpn.peerSubnet`,
      initialValue: '10.99.99.0/24',
      rules: [{ ...regRulesMap.subnet }],
    },
    {
      label: i18n.t('cmp:platform-vpn network segment'),
      name: `${formPrefix}.openvpn.subnets`,
      type: 'select',
      required: false,
      itemProps: {
        mode: 'tags',
        tokenSeparators: [';'],
        dropdownStyle: { display: 'none' },
        placeholder: i18n.t('cmp:Please input client segment, separated by pressing Enter or semicolon.'),
      },
      rules: [
        {
          validator: (_rule: any, value: any, callback: Function) => {
            let pass = true;
            if (value) {
              value.forEach((item: string) => {
                const o = item.replace(/\s+/g, '');
                o !== '' && (pass = regRulesMap.subnet.pattern.test(o));
              });
            }
            return pass
              ? callback()
              : callback(i18n.t('cmp:Please fill in the correct network segment, separated by pressing Enter.'));
          },
        },
      ],
    },
  ];
  const MySQLField = [
    {
      label: i18n.t('cmp:MySQL Host'),
      name: `${formPrefix}.mysql.host`,
      required: false,
      rules: [{ ...regRulesMap.ip }],
    },
    {
      label: i18n.t('cmp:MySQL Port'),
      name: `${formPrefix}.mysql.port`,
      initialValue: 3306,
      required: false,
      rules: [{ ...regRulesMap.port }],
    },
    {
      label: i18n.t('cmp:MySQL User'),
      required: false,
      name: `${formPrefix}.mysql.username`,
    },
    {
      label: i18n.t('cmp:MySQL Password'),
      required: false,
      name: `${formPrefix}.mysql.password`,
    },
    {
      label: i18n.t('cmp:MySQL Erda database'),
      required: false,
      name: `${formPrefix}.mysql.diceDB`,
      initialValue: 'dice',
    },
    {
      label: i18n.t('cmp:MySQL pandora name'),
      required: false,
      name: `${formPrefix}.mysql.pandoraDB`,
      initialValue: 'pandora',
    },
    {
      label: i18n.t('cmp:MySQL sonar name'),
      required: false,
      name: `${formPrefix}.mysql.sonarDB`,
      initialValue: 'sonar',
    },
  ];

  const isSaaS = form.getFieldValue('saas');
  if (!isSaaS) {
    fieldsList = fieldsList.concat(MySQLField as any);
  }
  return (
    <FormUnitContainer title={i18n.t('platform configs')} curRef={curRef}>
      {isReadonly ? (
        <ReadonlyForm fieldsList={fieldsList} data={data} />
      ) : (
        <RenderPureForm list={fieldsList} form={form} layout="vertical" className="deploy-form-render" />
      )}
    </FormUnitContainer>
  );
};

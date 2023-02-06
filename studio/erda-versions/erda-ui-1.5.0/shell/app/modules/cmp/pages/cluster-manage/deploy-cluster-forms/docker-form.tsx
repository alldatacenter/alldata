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

// docker配置
export const DockerForm = ({ form, isReadonly, data, curRef }: IFormProps) => {
  const formPrefix = 'config.docker';
  const fieldsList = [
    {
      label: i18n.t('cmp:data absolute path'),
      name: `${formPrefix}.dataRoot`,
      initialValue: '/var/lib/docker',
      itemProps: {
        placeholder: i18n.t('cmp:cannot be the same as the exec path'),
      },
      rules: [
        { ...regRulesMap.absolutePath },
        {
          validator: (_rule: any, value: any, callback: Function) => {
            let pass = true;
            if (value) {
              const execRoot = form.getFieldValue(['config', 'docker', 'execRoot']);
              if (value === execRoot) pass = false;
            }
            return pass ? callback() : callback(i18n.t('cmp:cannot be the same as the exec path'));
          },
        },
      ],
    },
    {
      label: i18n.t('cmp:exec absolute path'),
      name: `${formPrefix}.execRoot`,
      initialValue: '/var/run/docker',
      itemProps: {
        placeholder: i18n.t("cmp:can't be the same as the data path"),
      },
      rules: [
        { ...regRulesMap.absolutePath },
        {
          validator: (_rule: any, value: any, callback: Function) => {
            let pass = true;
            if (value) {
              const dataRoot = form.getFieldValue(['config', 'docker', 'dataRoot']);
              if (value === dataRoot) pass = false;
            }
            return pass ? callback() : callback(i18n.t("cmp:can't be the same as the data path"));
          },
        },
      ],
    },
    {
      label: i18n.t('cmp:Bip segment'),
      name: `${formPrefix}.bip`,
      initialValue: '172.17.0.1/16',
      rules: [{ ...regRulesMap.subnet }],
    },
    {
      label: i18n.t('cmp:CIDR segment'),
      name: `${formPrefix}.fixedCIDR`,
      initialValue: '172.17.0.0/16',
      rules: [{ ...regRulesMap.subnet }],
    },
  ];
  return (
    <FormUnitContainer title={i18n.t('cmp:docker configs')} curRef={curRef}>
      {isReadonly ? (
        <ReadonlyForm fieldsList={fieldsList} data={data} />
      ) : (
        <RenderPureForm list={fieldsList} form={form} layout="vertical" className="deploy-form-render" />
      )}
    </FormUnitContainer>
  );
};

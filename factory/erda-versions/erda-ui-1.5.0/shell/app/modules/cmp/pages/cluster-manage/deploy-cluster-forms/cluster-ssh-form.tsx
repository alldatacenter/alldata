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

// 集群内SSH信息
export const ClusterSSHForm = ({ form, isReadonly, data, curRef }: IFormProps) => {
  const formPrefix = 'config.ssh';
  const fieldsList = [
    {
      label: i18n.t('port'),
      name: `${formPrefix}.port`,
      initialValue: 22,
      rules: [{ ...regRulesMap.port }],
    },
    {
      label: i18n.t('user name'),
      name: `${formPrefix}.user`,
      initialValue: 'root',
    },
    {
      label: i18n.t('password'),
      name: `${formPrefix}.password`,
      itemProps: { placeholder: i18n.t('cmp:leave blank if a trust relationship has been established') },
      required: false,
    },
    {
      label: i18n.t('cmp:account used for installation and operation'),
      name: `${formPrefix}.account`,
      initialValue: 'dice',
    },
  ];
  return (
    <FormUnitContainer title={i18n.t('cmp:cluster ssh infos')} curRef={curRef}>
      {isReadonly ? (
        <ReadonlyForm fieldsList={fieldsList} data={data} />
      ) : (
        <RenderPureForm list={fieldsList} form={form} layout="vertical" className="deploy-form-render" />
      )}
    </FormUnitContainer>
  );
};

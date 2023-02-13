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
import { get } from 'lodash';
import { RenderPureForm, ReadonlyForm } from 'common';
import { Switch } from 'antd';

// orgID,跳板机（部署起点）
export const JumpBoardForm = ({ form, curRef, data, isReadonly }: IFormProps) => {
  const [isPassword, setIsPassword] = React.useState(true);

  React.useEffect(() => {
    const jumpData = get(data, 'jump') || {};
    if (jumpData.privateKey && !jumpData.password) {
      // 秘钥
      setIsPassword(false);
    } else {
      setIsPassword(true);
    }
  }, [data]);

  const formPrefix = 'jump';
  const fieldsList = [
    {
      name: 'saas',
      initialValue: true,
      type: 'switch',
      itemProps: {
        type: 'hidden',
      },
    },
    {
      label: i18n.t('host IP'),
      name: `${formPrefix}.host`,
      rules: [{ ...regRulesMap.ip }],
    },
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
      getComp: () => (
        <Switch
          checkedChildren={i18n.t('password')}
          unCheckedChildren={i18n.t('privateKey')}
          checked={isPassword}
          onClick={() => setIsPassword(!isPassword)}
        />
      ),
    },
    isPassword
      ? {
          label: i18n.t('password'),
          name: `${formPrefix}.password`,
        }
      : {
          label: i18n.t('privateKey'),
          name: `${formPrefix}.privateKey`,
          type: 'textArea',
        },
  ];
  return (
    <FormUnitContainer title={i18n.t('jump server')} curRef={curRef}>
      {isReadonly ? (
        <ReadonlyForm fieldsList={fieldsList} data={data} />
      ) : (
        <RenderPureForm list={fieldsList} form={form} layout="vertical" className="deploy-form-render" />
      )}
    </FormUnitContainer>
  );
};

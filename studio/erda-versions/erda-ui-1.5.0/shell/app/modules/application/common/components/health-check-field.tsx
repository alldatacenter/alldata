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
import { Input, InputNumber, Select } from 'antd';
import { useUpdate } from 'common/use-hooks';
import i18n from 'i18n';
import './variable-input-group.scss';

const { Option } = Select;

interface IVariableInputGroupProps {
  value: Obj;
  disabled?: boolean;
  onChange: (val: Obj) => void;
}

const defaultValue = {
  healthCheckKey: null,
};

const HealthCheckField = (props: IVariableInputGroupProps) => {
  const { value: propsValue, disabled, onChange } = props;
  const [{ healthCheckKey, value }, , update] = useUpdate({
    healthCheckKey: Object.keys(propsValue)?.[0],
    value: propsValue || defaultValue,
  });

  React.useEffect(() => {
    update({
      healthCheckKey: Object.keys(propsValue)?.[0],
      value: propsValue,
    });
  }, [propsValue, update]);

  const triggerChange = (changedValue: Obj) => {
    onChange?.(changedValue);
  };

  const changeType = (v: string) => {
    triggerChange({
      [v]: {},
    });
  };

  const changeValue = (v: string | number | undefined, key: string) => {
    let _value = { ...value };
    if (!_value[healthCheckKey]) {
      _value = {
        [healthCheckKey]: {},
      };
    }
    _value[healthCheckKey][key] = v;
    triggerChange(_value);
  };

  let Comp = null;
  switch (healthCheckKey) {
    case 'http':
      Comp = <HttpComp disabled={disabled} value={value?.[healthCheckKey]} changeValue={changeValue} />;
      break;
    case 'exec':
      Comp = <CommandComp disabled={disabled} value={value?.[healthCheckKey]} changeValue={changeValue} />;
      break;
    default:
      break;
  }
  return (
    <div>
      <div>
        <span className="edit-service-label">{i18n.t('dop:health check mechanism')}: </span>
        <span>
          <Select disabled={disabled} value={healthCheckKey} onChange={(e: string) => changeType(e)}>
            <Option value="http">HTTP</Option>
            <Option value="exec">COMMAND</Option>
          </Select>
        </span>
      </div>
      {Comp}
    </div>
  );
};

interface IProps {
  disabled?: boolean;
  value?: { path: string; port: number; duration: number };
  changeValue: (v: string | number | undefined, key: string) => void;
}

const HttpComp = (props: IProps) => {
  const { disabled, value, changeValue } = props;
  return (
    <div>
      <div>
        <span className="edit-service-label">{i18n.t('port')}: </span>
        <span>
          <InputNumber
            disabled={disabled}
            min={1}
            className="w-full"
            value={value?.port}
            onChange={(v?: number) => changeValue(v, 'port')}
            placeholder={i18n.t('dop:please enter the port')}
          />
        </span>
      </div>
      <div>
        <span className="edit-service-label">URI {i18n.t('dop:path')}: </span>
        <span>
          <Input
            disabled={disabled}
            value={value?.path}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => changeValue(e.target.value, 'path')}
            placeholder={i18n.t('dop:please enter the path')}
          />
        </span>
      </div>
      <div>
        <span className="edit-service-label">{i18n.t('dop:duration')}: </span>
        <span>
          <InputNumber
            disabled={disabled}
            className="w-full"
            value={value?.duration || 0}
            onChange={(v?: number) => changeValue(v, 'duration')}
            placeholder={i18n.t('dop:please enter the duration')}
            min={1}
            formatter={(v: any) => `${v}秒`}
            parser={(v: any) => v.replace('秒', '')}
          />
        </span>
      </div>
    </div>
  );
};

interface ICommandProps {
  disabled?: boolean;
  value?: { cmd: string; duration: number };
  changeValue: (v: string | number | undefined, key: string) => void;
}
const CommandComp = (props: ICommandProps) => {
  const { disabled, value, changeValue } = props;
  return (
    <div>
      <div>
        <span className="edit-service-label">{i18n.t('dop:command')}: </span>
        <span>
          <Input
            disabled={disabled}
            value={value?.cmd}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => changeValue(e.target.value, 'cmd')}
            placeholder={i18n.t('dop:please enter the command')}
          />
        </span>
      </div>
      <div>
        <span className="edit-service-label">{i18n.t('dop:duration')}: </span>
        <span>
          <InputNumber
            disabled={disabled}
            className="w-full"
            value={value?.duration || 0}
            onChange={(v?: number) => changeValue(v, 'duration')}
            placeholder={i18n.t('dop:please enter the duration')}
            min={1}
            formatter={(v: any) => `${v}秒`}
            parser={(v: any) => v.replace('秒', '')}
          />
        </span>
      </div>
    </div>
  );
};

export default HealthCheckField;

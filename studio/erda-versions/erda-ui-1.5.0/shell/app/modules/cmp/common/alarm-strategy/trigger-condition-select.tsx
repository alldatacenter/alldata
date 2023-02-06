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
import { map } from 'lodash';
import { ErdaIcon } from 'common';
import { Select, Input, message } from 'antd';
import i18n from 'i18n';

const { Option } = Select;

interface IProps {
  keyOptions: COMMON_STRATEGY_NOTIFY.IAlertTriggerCondition[];
  id: string;
  current: {
    id: string;
    condition: string;
    operator: string;
    values: string;
    valueOptions: Array<{ key: string; display: string }>;
  };
  handleEditTriggerConditions: (id: string, data: { key: string; value: string }) => void;
  handleRemoveTriggerConditions: (id: string) => void;
  operatorOptions: Array<{ key: string; display: string; type: 'input' | 'none' | 'multiple' | 'single' }>;
  valueOptionsList: COMMON_STRATEGY_NOTIFY.IAlertTriggerConditionContent[];
}

export const TriggerConditionSelect = ({
  keyOptions,
  id,
  current,
  handleEditTriggerConditions,
  handleRemoveTriggerConditions,
  operatorOptions,
  valueOptionsList,
}: IProps) => {
  const { type } = operatorOptions.find((t) => t.key === current.operator) ?? operatorOptions[0];

  return (
    <div className="flex items-center mb-4 last:mb-0">
      <Select
        placeholder={i18n.t('cmp:select label')}
        className="mr-8"
        style={{ width: 200 }}
        value={current?.condition}
        onSelect={(value) => {
          handleEditTriggerConditions(id, { key: 'condition', value });
          const currentOptions =
            valueOptionsList
              .find((item: { key: string }) => item.key === value)
              ?.options.map((item: string) => ({ key: item, display: item })) ?? [];
          if (currentOptions.length === 0) {
            message.warning(
              i18n.t('cmp:There is no option under this tab, you can choose the matching mode to input data'),
            );
          }
          handleEditTriggerConditions(id, { key: 'valueOptions', value: currentOptions });
          handleEditTriggerConditions(id, { key: 'values', value: currentOptions[0]?.key });
        }}
      >
        {map(keyOptions, (item) => {
          return (
            <Option key={item?.key} value={item?.key}>
              {item?.displayName}
            </Option>
          );
        })}
      </Select>
      <Select
        className="mr-8 flex-grow-0"
        style={{ width: 120 }}
        value={current?.operator}
        onSelect={(value) => {
          handleEditTriggerConditions(id, { key: 'operator', value });
          handleEditTriggerConditions(id, {
            key: 'values',
            value: value === 'all' ? current?.valueOptions?.map((item) => item?.key)?.join(',') : undefined,
          });
        }}
      >
        {map(operatorOptions, (item) => {
          return (
            <Option key={item.key} value={item.key}>
              {item.display}
            </Option>
          );
        })}
      </Select>
      {['input', 'none'].includes(type) ? (
        <Input
          key={type}
          placeholder={type === 'input' ? i18n.t('cmp:please enter here') : ''}
          className="flex-grow-0"
          style={{ width: 420 }}
          disabled={type === 'none'}
          value={type === 'none' ? undefined : current?.values}
          onChange={(e) => {
            handleEditTriggerConditions(id, {
              key: 'values',
              value: type === 'none' ? current?.valueOptions?.map((item) => item?.key)?.join(',') : e.target.value,
            });
          }}
        />
      ) : (
        <Select
          value={type === 'single' ? current?.values : current?.values?.split(',')}
          disabled={current?.valueOptions?.length === 0}
          className="flex-grow-0"
          placeholder={i18n.t('please select')}
          style={{ width: 420 }}
          mode={type === 'single' ? undefined : type}
          onChange={(value) =>
            handleEditTriggerConditions(id, { key: 'values', value: type === 'single' ? value : value?.join(',') })
          }
        >
          {map(current?.valueOptions, (item) => {
            return (
              <Option key={item?.key} value={item?.key}>
                {item?.display}
              </Option>
            );
          })}
        </Select>
      )}
      <ErdaIcon
        type="reduce"
        className="cursor-pointer ml-2 text-darkgray hover:text-primary"
        size="20"
        onClick={() => handleRemoveTriggerConditions(id)}
      />
    </div>
  );
};

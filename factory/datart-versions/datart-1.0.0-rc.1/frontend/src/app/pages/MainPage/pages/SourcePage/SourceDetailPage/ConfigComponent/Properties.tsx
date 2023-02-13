/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { ProColumnType } from '@ant-design/pro-table';
import { Button } from 'antd';
import { Configuration } from 'app/components';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { useCallback, useMemo } from 'react';
import styled from 'styled-components';
import { LINE_HEIGHT_BODY } from 'styles/StyleConstants';
import { uuidv4 } from 'utils/utils';

interface Property {
  key: string;
  value: string;
}

interface PropertyWithID extends Property {
  id: string;
}

interface PropertiesProps {
  value?: object;
  disabled?: boolean;
  allowManage?: boolean;
  onChange?: (val: object) => void;
}

export function Properties({
  value: valueProp,
  disabled,
  allowManage,
  onChange,
}: PropertiesProps) {
  const t = useI18NPrefix('source');
  const tg = useI18NPrefix('global');

  const tableDataSource = useMemo(
    () =>
      valueProp
        ? Object.entries(valueProp).map(([key, value]) => ({
            id: uuidv4(),
            key,
            value,
          }))
        : [],
    [valueProp],
  );

  const change = useCallback(
    (arr: PropertyWithID[]) => {
      onChange &&
        onChange(
          arr.reduce((obj, { key, value }) => ({ ...obj, [key]: value }), {}),
        );
    },
    [onChange],
  );

  const columns: ProColumnType<PropertyWithID>[] = useMemo(
    () => [
      {
        title: 'Key',
        dataIndex: 'key',
        formItemProps: (_, { rowIndex }) => ({
          rules: [
            { required: true, message: `Key${tg('validation.required')}` },
            {
              validator: (_, val) => {
                if (!valueProp) {
                  return Promise.resolve();
                }
                if (
                  !val ||
                  !valueProp[val] ||
                  val === tableDataSource[rowIndex]?.key
                ) {
                  return Promise.resolve();
                }
                return Promise.reject(new Error(t('form.duplicateKey')));
              },
            },
          ],
        }),
      },
      {
        title: 'Value',
        dataIndex: 'value',
      },
      {
        title: tg('title.action'),
        valueType: 'option',
        width: 160,
        render: (_, record, __, action) => [
          <ActionButton
            key="editable"
            type="link"
            disabled={disabled || !allowManage}
            onClick={() => {
              action?.startEditable?.(record.id);
            }}
          >
            {tg('button.edit')}
          </ActionButton>,
          <ActionButton
            key="delete"
            type="link"
            disabled={disabled || !allowManage}
            onClick={() => {
              valueProp &&
                onChange &&
                onChange(
                  Object.entries(valueProp).reduce(
                    (newValue, [k, v]) =>
                      k === record.key ? newValue : { ...newValue, [k]: v },
                    {},
                  ),
                );
            }}
          >
            {tg('button.delete')}
          </ActionButton>,
        ],
      },
    ],
    [disabled, allowManage, valueProp, tableDataSource, onChange, t, tg],
  );
  return (
    <Configuration
      columns={columns}
      creatorButtonText={t('form.addProperty')}
      value={tableDataSource}
      disabled={disabled || !allowManage}
      onChange={change}
    />
  );
}

const ActionButton = styled(Button)`
  height: ${LINE_HEIGHT_BODY};
  padding: 0;
`;

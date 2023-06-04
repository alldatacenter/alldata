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

import { Form, Radio, RadioChangeEvent } from 'antd';
import { memo } from 'react';
import { PermissionLevels } from '../../constants';

interface IndependentPermissionSettingProps {
  enabled: PermissionLevels;
  label: string;
  extra?: string;
  values: Array<{ text: string; value: PermissionLevels }>;
  onChange: (e: RadioChangeEvent) => void;
}

export const IndependentPermissionSetting = memo(
  ({
    enabled,
    label,
    extra,
    values,
    onChange,
  }: IndependentPermissionSettingProps) => {
    return (
      <Form.Item label={label} tooltip={extra}>
        <Radio.Group value={enabled} onChange={onChange}>
          {values.map(({ text, value }) => (
            <Radio key={value} value={value}>
              {text}
            </Radio>
          ))}
        </Radio.Group>
      </Form.Item>
    );
  },
);

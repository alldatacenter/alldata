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
import { Form, Select } from 'antd';
import { NamePath } from 'rc-field-form/lib/interface';
import React, { FC, memo } from 'react';
export const SelectSet: FC<{
  options: any;
  value: string | undefined;
  defaultValue?: string;
  name: NamePath;
}> = memo(({ options, value, defaultValue, name }) => {
  return (
    <Form.Item label="" name={name}>
      <Select
        className="datart-ant-select"
        defaultValue={defaultValue}
        value={value}
      >
        {options.map(o => (
          <Select.Option key={o.value} value={o.value}>
            {o.name}
          </Select.Option>
        ))}
      </Select>
    </Form.Item>
  );
});

export default SelectSet;

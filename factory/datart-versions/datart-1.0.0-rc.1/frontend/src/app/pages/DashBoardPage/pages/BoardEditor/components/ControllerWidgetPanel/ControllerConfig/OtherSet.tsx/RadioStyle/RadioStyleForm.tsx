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
import { Form } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import React, { memo } from 'react';
import { RadioButtonTypeName } from '../..';
import { RadioStyleSet } from './RadioStyleSet';
export interface RadioStyleFormProps {}
export const RadioStyleForm: React.FC<RadioStyleFormProps> = memo(() => {
  const filterT = useI18NPrefix('viz.common.filter');
  const items = ['default', 'button'];
  const options = items.map(it => ({
    label: filterT(it),
    value: it,
  }));
  return (
    <Form.Item
      name={RadioButtonTypeName}
      label={filterT('radioType')}
      validateTrigger={['onChange', 'onBlur']}
      rules={[{ required: true }]}
    >
      <RadioStyleSet options={options} />
    </Form.Item>
  );
});

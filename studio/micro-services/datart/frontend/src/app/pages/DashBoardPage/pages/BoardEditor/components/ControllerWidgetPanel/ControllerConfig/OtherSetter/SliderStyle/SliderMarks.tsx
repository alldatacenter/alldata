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
import { Form, Switch } from 'antd';
import React, { memo } from 'react';
import { SliderShowMarksName } from '../..';

export const SliderMarks: React.FC<{
  label: string;
  switchTexts: [string, string];
}> = memo(({ label, switchTexts }) => {
  return (
    <Form.Item
      name={SliderShowMarksName}
      label={label}
      valuePropName="checked"
      validateTrigger={['onChange', 'onBlur']}
      rules={[{ required: true }]}
    >
      <Switch
        checkedChildren={switchTexts[0]}
        unCheckedChildren={switchTexts[1]}
      />
    </Form.Item>
  );
});

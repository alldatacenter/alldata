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

import { Form, FormInstance, Input, Radio, Select } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import {
  ALL_SQL_OPERATOR_OPTIONS,
  VISIBILITY_TYPE_OPTION,
} from 'app/pages/DashBoardPage/constants';
import { Widget } from 'app/pages/DashBoardPage/types/widgetTypes';
import { FilterSqlOperator } from 'globalConstants';
import { FC, memo, useCallback } from 'react';
import styled from 'styled-components/macro';
import { ControllerConfig } from '../types';

const ControllerVisibility: FC<{
  form: FormInstance<{ config: ControllerConfig }> | undefined;
  otherStrFilterWidgets: Widget[];
}> = memo(({ form, otherStrFilterWidgets }) => {
  const tc = useI18NPrefix('viz.control');
  const showVisibilityCondition = useCallback(() => {
    const visibilityType = form?.getFieldValue([
      'config',
      'visibility',
      'visibilityType',
    ]);
    if (visibilityType === 'condition') {
      return true;
    }
    return false;
  }, [form]);

  return (
    <Form.Item
      label={tc('visibility')}
      shouldUpdate
      rules={[{ required: true }]}
    >
      {() => {
        return (
          <>
            <Form.Item
              name={['config', 'visibility', 'visibilityType']}
              noStyle
              rules={[{ required: true }]}
            >
              <Radio.Group>
                {VISIBILITY_TYPE_OPTION.map(ele => {
                  return (
                    <Radio.Button key={ele.value} value={ele.value}>
                      {ele.name}
                    </Radio.Button>
                  );
                })}
              </Radio.Group>
            </Form.Item>
            {showVisibilityCondition() && (
              <Form.Item noStyle>
                <StyledWrapper>
                  <Form.Item
                    name={[
                      'config',
                      'visibility',
                      'condition',
                      'dependentControllerId',
                    ]}
                    noStyle
                    rules={[{ required: true, message: '' }]}
                  >
                    <Select
                      placeholder={tc('title')}
                      style={{ width: '160px' }}
                    >
                      {otherStrFilterWidgets.map(ele => {
                        return (
                          <Select.Option key={ele.id} value={ele.id}>
                            {ele.config.name}
                          </Select.Option>
                        );
                      })}
                    </Select>
                  </Form.Item>
                  {' - '}
                  <Form.Item
                    name={['config', 'visibility', 'condition', 'relation']}
                    noStyle
                    rules={[{ required: true, message: '' }]}
                  >
                    <Select placeholder="" style={{ width: '100px' }}>
                      {ALL_SQL_OPERATOR_OPTIONS.filter(it =>
                        [
                          FilterSqlOperator.Equal,
                          FilterSqlOperator.NotEqual,
                        ].includes(it.value),
                      ).map(it => {
                        return (
                          <Select.Option key={it.value} value={it.value}>
                            {it.name}
                          </Select.Option>
                        );
                      })}
                    </Select>
                  </Form.Item>
                  {' - '}
                  <Form.Item
                    name={['config', 'visibility', 'condition', 'value']}
                    noStyle
                    rules={[{ required: true, message: '' }]}
                  >
                    <Input style={{ width: '140px' }} />
                  </Form.Item>
                </StyledWrapper>
              </Form.Item>
            )}
          </>
        );
      }}
    </Form.Item>
  );
});

export default ControllerVisibility;
const StyledWrapper = styled(Form.Item)`
  display: block;
  margin-top: 6px;
  &.ant-form-item {
    margin-bottom: 0;
  }
`;

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
import { Form, InputNumber } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import React, { FC, memo } from 'react';
export const PaddingSet: FC<{}> = memo(() => {
  const t = useI18NPrefix(`viz.board.setting`);
  return (
    <>
      <Form.Item label={t('paddingTop')} name={['padding', 'top']}>
        <InputNumber className="datart-ant-input-number" />
      </Form.Item>
      <Form.Item label={t('paddingRight')} name={['padding', 'right']}>
        <InputNumber className="datart-ant-input-number" />
      </Form.Item>
      <Form.Item label={t('paddingBottom')} name={['padding', 'bottom']}>
        <InputNumber className="datart-ant-input-number" />
      </Form.Item>
      <Form.Item label={t('paddingLeft')} name={['padding', 'left']}>
        <InputNumber className="datart-ant-input-number" />
      </Form.Item>
    </>
  );
});

export default PaddingSet;

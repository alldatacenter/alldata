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
import { Form, InputNumber, Select } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { BORDER_STYLES } from 'app/pages/DashBoardPage/constants';
import { BorderConfig } from 'app/pages/DashBoardPage/pages/Board/slice/types';
import React, { FC, memo } from 'react';

export const BorderSet: FC<{
  border: BorderConfig;
}> = memo(({ border }) => {
  const t = useI18NPrefix(`viz.board.setting`);
  const tLine = useI18NPrefix(`viz.lineOptions`);
  return (
    <>
      {/* <Form.Item label={t('color')} name={['border', 'color']}>
        <ColorSet filedName={['border', 'color']} filedValue={border.color} />
      </Form.Item> */}

      <Form.Item label={t('width')} name={['border', 'width']}>
        <InputNumber className="datart-ant-input-number" />
      </Form.Item>
      <Form.Item label={t('style')} name={['border', 'style']}>
        <Select className="datart-ant-select">
          {BORDER_STYLES.map(item => (
            <Select.Option key={item} value={item}>
              {tLine(item)}
            </Select.Option>
          ))}
        </Select>
      </Form.Item>
      <Form.Item label={t('radius')} name={['border', 'radius']}>
        <InputNumber className="datart-ant-input-number" />
      </Form.Item>
    </>
  );
});

export default BorderSet;

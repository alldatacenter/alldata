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
import { MinusSquareOutlined, PlusSquareOutlined } from '@ant-design/icons';
import { Button, Slider, Space, Tooltip } from 'antd';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import React, { useMemo } from 'react';
import styled from 'styled-components/macro';

export interface ZoomControlProps {
  sliderValue: number;
  scale: [number, number];
  zoomIn: () => void;
  zoomOut: () => void;
  sliderChange: (newSliderValue: number) => void;
}
const ZoomControl: React.FC<ZoomControlProps> = props => {
  const { sliderValue, scale, zoomIn, zoomOut, sliderChange } = props;
  const percentage = useMemo(() => {
    if (!scale) {
      return '';
    }
    if (scale[0] === scale[1]) {
      return `${Math.floor(scale[0] * 100)}%`;
    }
    return scale.map((s: number) => `${Math.floor(s * 100)}%`).join('/');
  }, [scale]);
  const t = useI18NPrefix(`viz.board.action`);
  return (
    <Wrapper>
      <div className="bottom-box">
        <Space>
          <Tooltip title={t('zoomIn')}>
            <Button
              size="small"
              type="text"
              onClick={zoomIn}
              icon={<MinusSquareOutlined />}
            ></Button>
          </Tooltip>

          <Slider
            className="bottom-slider"
            onChange={sliderChange}
            value={sliderValue}
          />
          <Tooltip title={t('zoomOut')}>
            <Button
              size="small"
              type="text"
              onClick={zoomOut}
              icon={<PlusSquareOutlined />}
            ></Button>
          </Tooltip>

          <label className="value-label">{percentage}</label>
        </Space>
      </div>
    </Wrapper>
  );
};
export default ZoomControl;
const Wrapper = styled.div`
  display: flex;
  flex-direction: row-reverse;
  justify-content: space-between;
  height: 20px;
  background-color: ${p => p.theme.componentBackground};

  & .bottom-slider {
    display: inline-block;
    width: 200px;
    margin: 0;
  }

  & .value-label {
    margin-right: 20px;
  }
`;

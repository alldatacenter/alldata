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
import { InputNumber, Select } from 'antd';
import { ColorPickerPopover } from 'app/components/ColorPicker';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { BORDER_STYLES } from 'app/pages/DashBoardPage/constants';
import { BorderConfig } from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { ChartStyleConfig } from 'app/types/ChartConfig';
import { FC, memo, useEffect, useRef } from 'react';
import styled from 'styled-components/macro';
import { BW } from '../../Basic/components/BasicWrapper';
import { Group, WithColorPicker } from '../../Basic/components/Group';
import { ItemLayoutProps } from '../../types';

export const WidgetBorder: FC<ItemLayoutProps<ChartStyleConfig>> = memo(
  ({ ancestors, translate: t = title => title, data, onChange, ...rest }) => {
    const { value, options } = data;
    const gt = useI18NPrefix(`viz.board.setting`);
    const tLine = useI18NPrefix(`viz.lineOptions`);
    const valRef = useRef<BorderConfig>();
    useEffect(() => {
      valRef.current = value;
    }, [value]);
    const handleChange = (obj: { key: string; val: string }) => {
      const newVal = {
        ...valRef.current,
        [obj.key]: obj.val,
      };
      onChange?.(ancestors, newVal);
    };
    const changeW = val => handleChange({ key: 'width', val });
    const changeR = val => handleChange({ key: 'radius', val });
    const changeC = val => handleChange({ key: 'color', val });
    const changeS = val => handleChange({ key: 'style', val });

    return (
      <Wrap>
        <Group>
          <label className="label">{gt('color')}</label>
          <WithColorPicker>
            <ColorPickerPopover
              {...rest}
              {...options}
              defaultValue={data.value?.color}
              onSubmit={changeC}
            />
          </WithColorPicker>
        </Group>

        <BW label={gt('width')}>
          <Group>
            <InputNumber
              value={data.value?.width}
              className="datart-ant-input-number"
              onChange={changeW}
            />
          </Group>
        </BW>
        <BW label={gt('radius')}>
          <Group>
            <InputNumber
              value={data.value?.radius}
              className="datart-ant-input-number"
              onChange={changeR}
            />
          </Group>
        </BW>
        <BW label={gt('style')}>
          <Group>
            <Select
              className="datart-ant-select"
              value={data.value?.style}
              onChange={changeS}
            >
              {BORDER_STYLES.map(item => (
                <Select.Option key={item} value={item}>
                  {tLine(item)}
                </Select.Option>
              ))}
            </Select>
          </Group>
        </BW>
      </Wrap>
    );
  },
);
const Wrap = styled.div`
  display: block;
  .ant-upload-list {
    display: none;
  }
  .label {
    display: block;
    width: 60px;
  }
  .datart-ant-select {
    flex: 1;
  }
  .datart-ant-input-number {
    flex: 1;
  }
`;

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
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { ITimeDefault } from 'app/pages/DashBoardPage/types/widgetTypes';
import { ChartStyleConfig } from 'app/types/ChartConfig';
import { FC, memo, useEffect, useRef } from 'react';
import styled from 'styled-components/macro';
import { BW } from '../../Basic/components/BasicWrapper';
import { Group } from '../../Basic/components/Group';
import { ItemLayoutProps } from '../../types';
const TIME_FORMATS = [
  'YYYY-MM-DD HH:mm:ss',
  'YYYY-MM-DD HH:mm',
  'YYYY-MM-DD HH',
  'YYYY-MM-DD',
  'MM-DD',
  'YYYY-MM',
  'YYYY',
  'HH:mm:ss',
  'HH:mm',
  'mm:ss',
  'mm:ss.S',
  'mm:ss.SS',
  'mm:ss.SSS',
  'mm:ss.SSSS',
  'M',
  'MM',
  'MMM',
  'MMMM',
  'Q',
  'W',
  'D',
  'DD',
  'DDD',
  'DDDD',
  'dddd',
  'ddd',
  'dd',
  'd',
  'A',
  'a',
  'H',
  'HH',
  'h',
  'hh',
  'm',
  'mm',
  's',
  'ss',
  'S',
  'SS',
  'SSS',
  'SSSS',
];
export const TimerFormat: FC<ItemLayoutProps<ChartStyleConfig>> = memo(
  ({ ancestors, translate: t = title => title, data, onChange, ...rest }) => {
    const { value } = data;

    const gt = useI18NPrefix(`viz.board.setting`);
    const valRef = useRef<ITimeDefault>();
    const selectRef = useRef<any>();
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
    const changeF = val => {
      if (Array.isArray(val)) {
        if (val.length > 0) {
          selectRef.current.blur();
          if (val.length > 1) {
            val.shift();
          }
        }
      }
      handleChange({ key: 'format', val: val[0] });
    };
    const changeD = val => handleChange({ key: 'duration', val });

    return (
      <Wrap>
        <BW label={gt('format')}>
          <Group>
            <Select
              ref={selectRef}
              mode="tags"
              className="datart-ant-select"
              maxTagCount={1}
              value={value.format}
              onChange={changeF}
            >
              {TIME_FORMATS.map(item => (
                <Select.Option key={item} value={item}>
                  {item}
                </Select.Option>
              ))}
            </Select>
          </Group>
        </BW>
        <BW label={gt('interval')}>
          <Group>
            <InputNumber
              value={value.duration}
              className="datart-ant-input-number"
              onChange={changeD}
            />
          </Group>
        </BW>
      </Wrap>
    );
  },
);
const Wrap = styled.div`
  display: block;

  .datart-ant-select {
    flex: 1;
  }
  .datart-ant-input-number {
    flex: 1;
  }
`;

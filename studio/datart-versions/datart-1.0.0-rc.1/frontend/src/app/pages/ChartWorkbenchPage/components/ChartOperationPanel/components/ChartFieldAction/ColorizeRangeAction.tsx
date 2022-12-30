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

import { Checkbox, Col, Row } from 'antd';
import { ColorPickerPopover } from 'app/components/ColorPicker';
import { ColorPicker } from 'app/components/ColorPicker/ColorTag';
import { FormItemEx } from 'app/components/Form';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { ChartDataSectionField } from 'app/types/ChartConfig';
import ChartDataSetDTO from 'app/types/ChartDataSet';
import { updateBy } from 'app/utils/mutation';
import { FC, memo, useState } from 'react';
import styled from 'styled-components/macro';

const ColorizeRangeAction: FC<{
  config: ChartDataSectionField;
  dataset?: ChartDataSetDTO;
  onConfigChange: (
    config: ChartDataSectionField,
    needRefresh?: boolean,
  ) => void;
}> = memo(({ config, onConfigChange }) => {
  const actionNeedNewRequest = false;
  const t = useI18NPrefix(`viz.palette.data.actions`);

  const [colorRange, setColorRange] = useState<{
    start?: string;
    end?: string;
  }>(config?.color!);

  const handleColorRangeChange = (start?, end?) => {
    const newConfig = updateBy(config, draft => {
      if (!start && !end) {
        delete draft.color;
      } else {
        draft.color = { start, end };
      }
    });
    setColorRange(newConfig?.color!);
    onConfigChange?.(newConfig, actionNeedNewRequest);
  };

  const handleEnableColorChecked = checked => {
    if (Boolean(checked)) {
      handleColorRangeChange('#7567bd', '#7567bd');
    } else {
      handleColorRangeChange();
    }
  };

  return (
    <StyledColorizeRangeAction>
      <Col span={24}>
        <Checkbox
          checked={!!colorRange?.start || !!colorRange?.end}
          onChange={e => handleEnableColorChecked(e.target?.checked)}
        >
          {t('color.enable')}
        </Checkbox>
      </Col>
      <Col span={12}>
        <Row align="middle">
          <FormItemEx
            label={t('color.start')}
            name="StartColor"
            rules={[{ required: true }]}
            initialValue={colorRange?.start}
            className="form-item-ex"
          >
            <ColorPickerPopover
              onChange={v => {
                handleColorRangeChange(v, colorRange?.end);
              }}
            >
              <ColorPicker className="ColorPicker" color={colorRange?.start} />
            </ColorPickerPopover>
          </FormItemEx>
        </Row>
      </Col>
      <Col span={12}>
        <Row align="middle">
          <FormItemEx
            label={t('color.end')}
            name="EndColor"
            rules={[{ required: true }]}
            initialValue={colorRange?.end}
            className="form-item-ex"
          >
            <ColorPickerPopover
              onChange={v => {
                handleColorRangeChange(colorRange?.start, v);
              }}
            >
              <ColorPicker className="ColorPicker" color={colorRange?.end} />
            </ColorPickerPopover>
          </FormItemEx>
        </Row>
      </Col>
    </StyledColorizeRangeAction>
  );
});

export default ColorizeRangeAction;

const StyledColorizeRangeAction = styled(Row)`
  justify-content: center;
  .ColorPicker {
    border: 1px solid ${p => p.theme.borderColorBase};
  }
  .form-item-ex {
    width: 100%;
    .ant-form-item-control-input {
      width: auto;
    }
  }
`;

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

import { Col, Popover, Row } from 'antd';
import Theme from 'app/assets/theme/echarts_default_theme.json';
import {
  ColorTag,
  SingleColorSelection,
  ThemeColorSelection,
} from 'app/components/ColorPicker';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { ChartDataSectionField } from 'app/types/ChartConfig';
import ChartDataSetDTO from 'app/types/ChartDataSet';
import { updateBy } from 'app/utils/mutation';
import { FC, memo, useState } from 'react';
import styled from 'styled-components/macro';
import { SPACE_MD, SPACE_XS } from 'styles/StyleConstants';

const AggregationColorizeAction: FC<{
  config: ChartDataSectionField;
  dataset?: ChartDataSetDTO;
  onConfigChange: (
    config: ChartDataSectionField,
    needRefresh?: boolean,
  ) => void;
  i18nPrefix?: string;
}> = memo(({ config, dataset, onConfigChange, i18nPrefix }) => {
  const actionNeedNewRequest = true;
  const [themeColors, setThemeColors] = useState(Theme.color);
  const [colors, setColors] = useState<{ key: string; value: string }[]>(
    setColorFn(config, dataset, themeColors),
  );
  const [selectColor, setSelectColor] = useState(colors[0]);
  const [selColorBoxStatus, setSelColorBoxStatus] = useState(false);
  const t = useI18NPrefix(i18nPrefix);

  const handleColorChange = value => {
    if (selectColor) {
      const currentSelectColor = Object.assign({}, selectColor, {
        value,
      });
      const newColors = updateBy(colors, draft => {
        const index = draft.findIndex(r => r.key === selectColor.key) || 0;
        draft[index] = currentSelectColor;
      });

      setColors(newColors);
      setSelectColor(currentSelectColor);
      onConfigChange(
        updateBy(config, draft => {
          draft.color = { colors: newColors };
        }),
        actionNeedNewRequest,
      );
    }

    setSelColorBoxStatus(false);
  };

  const renderGroupColors = () => {
    return (
      <>
        {colors.map(c => (
          <li
            key={c.key}
            onClick={(e: any) => {
              setSelColorBoxStatus(true);
              setSelectColor(c);
            }}
          >
            <ColorTag size={16} color={c.value} />
            <span className="text-span" title={c.key}>
              {c.key}
            </span>
          </li>
        ))}
      </>
    );
  };

  const selectThemeColorFn = colorArr => {
    let selectColor1 = setColorFn(config, dataset, colorArr, true);

    setThemeColors(colorArr);
    setColors(selectColor1);
    onConfigChange(
      updateBy(config, draft => {
        draft.color = { colors: selectColor1 };
      }),
      actionNeedNewRequest,
    );
  };
  return (
    <>
      <ThemeColorSelection callbackFn={selectThemeColorFn}>
        {t('chooseTheme')}
      </ThemeColorSelection>
      <Row>
        <Col span={24}>
          <StyledUL>{renderGroupColors()}</StyledUL>
        </Col>
        <Popover
          destroyTooltipOnHide
          onVisibleChange={setSelColorBoxStatus}
          visible={selColorBoxStatus}
          trigger="click"
          placement="bottomRight"
          overlayClassName="datart-aggregation-colorpopover"
          content={
            <SingleColorSelection
              color={selectColor.value}
              onChange={handleColorChange}
            />
          }
        ></Popover>
      </Row>
    </>
  );
});

export default AggregationColorizeAction;

function setColorFn(
  config,
  dataset,
  themeColors,
  need = false,
): { key: string; value: string }[] {
  const colorizedColumnName = config.colName;
  const colorizeIndex =
    dataset?.columns?.findIndex(r => r.name[0] === colorizedColumnName) || 0;
  const colorizedGroupValues = Array.from(
    new Set(dataset?.rows?.map(r => String(r[colorizeIndex]))),
  );
  const originalColors = config.color?.colors || [];
  const themeColorTotalCount = themeColors.length;

  return colorizedGroupValues
    .filter(Boolean)
    .map((k, index): { key: string; value: string } => {
      return {
        key: k! as string,
        value: need
          ? themeColors[index % themeColorTotalCount]
          : originalColors.find(pc => pc.key === k)?.value ||
            themeColors[index % themeColorTotalCount],
      };
    });
}

const StyledUL = styled.ul`
  max-height: 300px;
  padding-inline-start: 0;
  overflow: auto;
  li {
    display: flex;
    flex-direction: row;
    flex-wrap: nowrap;
    align-items: center;
    justify-content: flex-start;
    padding-right: ${SPACE_MD};
    cursor: pointer;
    .text-span {
      margin-left: ${SPACE_XS};
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    &:hover {
      background-color: rgba(27, 154, 238, 0.05);
    }
  }
`;

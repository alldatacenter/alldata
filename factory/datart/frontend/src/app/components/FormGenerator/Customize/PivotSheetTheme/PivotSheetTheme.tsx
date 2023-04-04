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

import { ChartStyleConfig } from 'app/types/ChartConfig';
import { updateByKey } from 'app/utils/mutation';
import { FC, memo, useCallback, useEffect, useMemo } from 'react';
import styled from 'styled-components/macro';
import { SPACE } from '../../../../../styles/StyleConstants';
import { ItemLayout } from '../../Layout';
import { ItemLayoutProps } from '../../types';
import { itemLayoutComparer } from '../../utils';
import { PIVOT_THEME_LIST, PIVOT_THEME_SELECT } from './theme';

const template = [
  {
    label: 'theme.colHeaderFontColor',
    key: 'colHeaderFontColor',
    index: 0,
  },
  {
    label: 'theme.colHeaderBgColor',
    key: 'colHeaderBgColor',
    index: 3,
  },
  {
    label: 'theme.colHeaderBgColorHover',
    key: 'colHeaderBgColorHover',
    index: 4,
  },
  {
    label: 'theme.colHeaderBorderColor',
    key: 'colHeaderBorderColor',
    index: 10,
  },
  {
    label: 'theme.rowHeaderFontColor',
    key: 'rowHeaderFontColor',
    index: 14,
  },
  {
    label: 'theme.cellFontColor',
    key: 'cellFontColor',
    index: 13,
  },
  {
    label: 'theme.cellBgColorHover',
    key: 'cellBgColorHover',
    index: 2,
  },
  {
    label: 'theme.cellBorderColor',
    key: 'cellBorderColor',
    index: 9,
  },
  {
    label: 'theme.oddBgColor',
    key: 'oddBgColor',
    index: 1,
  },
  {
    label: 'theme.evenBgColor',
    key: 'evenBgColor',
    index: 8,
  },
  {
    label: 'theme.verticalSplitLineColor',
    key: 'verticalSplitLineColor',
    index: 11,
  },
  {
    label: 'theme.horizontalSplitLineColor',
    key: 'horizontalSplitLineColor',
    index: 12,
  },
  {
    label: 'theme.resizeAreaColor',
    key: 'resizeAreaColor',
    index: 7,
  },
  {
    label: 'theme.prepareSelectMaskBgColor',
    key: 'prepareSelectMaskBgColor',
    index: 5,
  },
  {
    label: 'theme.linkTextColor',
    key: 'linkTextColor',
    index: 6,
  },
];

const themeSelectTemplate = {
  label: 'theme.themeType',
  key: 'themeType',
  comType: 'select',
  default: 0,
  options: {
    translateItemLabel: true,
    items: PIVOT_THEME_SELECT,
  },
};

const PivotSheetTheme: FC<ItemLayoutProps<ChartStyleConfig>> = memo(
  ({ ancestors, translate: t = title => title, data, onChange }) => {
    const handleSettingChange = useCallback(
      value => {
        const newData = updateByKey(data, 'value', value);
        onChange?.(ancestors, newData);
      },
      [onChange, ancestors, data],
    );

    useEffect(() => {
      if (!data.value?.colors?.length) {
        handleSettingChange({
          themeType: data.value?.themeType || 0,
          colors: PIVOT_THEME_LIST[data.value?.themeType || 0],
        });
      }
    }, [data.value?.themeType, data.value, handleSettingChange]);

    const handlePickerSelect = useCallback(
      index => (_, colorConfig) => {
        const newColors = data.value?.colors?.concat();
        newColors[index] = colorConfig.value;
        handleSettingChange({
          themeType: data.value.themeType,
          colors: newColors,
        });
      },
      [handleSettingChange, data],
    );

    const handleThemeSelect = useCallback(
      (_, config) => {
        handleSettingChange({
          themeType: config.value,
          colors: PIVOT_THEME_LIST[config.value],
        });
      },
      [handleSettingChange],
    );

    const themePropsList: ItemLayoutProps<ChartStyleConfig>[] = useMemo(() => {
      const themeSelectProps: ItemLayoutProps<ChartStyleConfig>[] = [
        {
          ancestors,
          translate: t,
          data: {
            label: data?.label || themeSelectTemplate.label,
            default: data?.default?.themeType || themeSelectTemplate.default,
            key: data.key || themeSelectTemplate.key,
            comType: 'select',
            options: data?.options || themeSelectTemplate.options,
            value: data?.value?.themeType,
          },
          onChange: handleThemeSelect,
          flatten: true,
        },
      ];
      const colorsProps: ItemLayoutProps<ChartStyleConfig>[] = template.map(
        item => ({
          ancestors,
          translate: t,
          data: {
            value: data.value?.colors[item.index],
            label: item.label,
            key: item.key,
            comType: 'fontColor',
          },
          onChange: handlePickerSelect(item.index),
          flatten: true,
        }),
      );
      return themeSelectProps.concat(colorsProps);
    }, [data, handleThemeSelect, handlePickerSelect, t, ancestors]);

    return (
      <>
        {themePropsList.map(props => (
          <StyledItemLayout key={props.data.key}>
            <ItemLayout {...props} />
          </StyledItemLayout>
        ))}
      </>
    );
  },
  itemLayoutComparer,
);

export default PivotSheetTheme;

const StyledItemLayout = styled.div`
  padding: ${SPACE} 0 ${SPACE} 0;
  user-select: none;
`;

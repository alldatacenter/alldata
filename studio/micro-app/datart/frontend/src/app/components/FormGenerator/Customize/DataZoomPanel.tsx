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
import { updateBy } from 'app/utils/mutation';
import { FC, memo, useEffect } from 'react';
import styled from 'styled-components/macro';
import { CloneValueDeep, mergeDefaultToValue } from 'utils/object';
import { FormGroupLayoutMode } from '../constants';
import { GroupLayout } from '../Layout';
import { ItemLayoutProps } from '../types';
import { itemLayoutComparer } from '../utils';

const template = {
  label: '',
  key: 'dataZoom',
  comType: 'dataZoomPanel',
  rows: [
    {
      label: 'viz.palette.style.dataZoomSlider.showZoomSlider',
      key: 'showZoomSlider',
      default: false,
      comType: 'checkbox',
    },
    {
      label: 'viz.palette.style.dataZoomSlider.zoomSliderColor',
      key: 'zoomSliderColor',
      comType: 'fontColor',
      default: '#8fb0f7',
    },
    {
      label: 'viz.palette.style.dataZoomSlider.usePercentage',
      key: 'usePercentage',
      default: true,
      comType: 'checkbox',
    },
    {
      label: 'viz.palette.style.dataZoomSlider.zoomStartPercentage',
      key: 'zoomStartPercentage',
      comType: 'inputNumber',
      default: 0,
      options: {
        step: 1,
        min: 0,
        max: 100,
      },
      watcher: {
        deps: ['usePercentage'],
        action: props => {
          return {
            disabled: !props.usePercentage,
          };
        },
      },
    },
    {
      label: 'viz.palette.style.dataZoomSlider.zoomEndPercentage',
      key: 'zoomEndPercentage',
      comType: 'inputNumber',
      default: 100,
      options: {
        step: 1,
        min: 0,
        max: 100,
      },
      watcher: {
        deps: ['usePercentage'],
        action: props => {
          return {
            disabled: !props.usePercentage,
          };
        },
      },
    },
    {
      label: 'viz.palette.style.dataZoomSlider.zoomStartIndex',
      key: 'zoomStartIndex',
      comType: 'inputNumber',
      default: undefined,
      options: {
        min: 0,
      },
      watcher: {
        deps: ['usePercentage'],
        action: props => {
          return {
            disabled: props.usePercentage,
          };
        },
      },
    },
    {
      label: 'viz.palette.style.dataZoomSlider.zoomEndIndex',
      key: 'zoomEndIndex',
      comType: 'inputNumber',
      default: undefined,
      options: {
        min: 0,
      },
      watcher: {
        deps: ['usePercentage'],
        action: props => {
          return {
            disabled: props.usePercentage,
          };
        },
      },
    },
  ],
};

const DataZoomPanel: FC<ItemLayoutProps<ChartStyleConfig>> = memo(
  ({
    ancestors,
    translate: t = title => title,
    data,
    dataConfigs,
    onChange,
  }) => {
    useEffect(() => {
      // init default component rows
      if (!data?.rows?.length) {
        const newData = updateBy(data, draft => {
          const temp = CloneValueDeep(template);
          draft.label = data?.label || temp.label;
          draft.key = data?.key || temp.key;
          draft.options = data?.options;
          draft.comType = temp.comType;
          draft.rows = mergeDefaultToValue(data?.rows || temp.rows);
        });
        onChange?.(ancestors, newData);
      }
    }, [ancestors, data, onChange]);

    const props = {
      ancestors,
      data,
      translate: t,
      onChange,
      dataConfigs,
      flatten: true,
      mode: FormGroupLayoutMode.INNER,
    };

    return (
      <StyledItemLayout>
        <GroupLayout {...props} />
      </StyledItemLayout>
    );
  },
  itemLayoutComparer,
);

export default DataZoomPanel;

const StyledItemLayout = styled.div``;

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
import { Form, FormInstance, Input } from 'antd';
import { ControllerFacadeTypes } from 'app/constants';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import {
  BoardType,
  ControllerWidgetContent,
} from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { Widget } from 'app/pages/DashBoardPage/types/widgetTypes';
import ChartDataView from 'app/types/ChartDataView';
import React, { memo, useMemo } from 'react';
import styled from 'styled-components/macro';
import ControllerVisibility from './ControllerVisibility';
import { RadioStyleForm } from './OtherSetter/RadioStyle/RadioStyleForm';
import { SliderMarks } from './OtherSetter/SliderStyle/SliderMarks';
import { SliderStep } from './OtherSetter/SliderStyle/SliderStep';
import { SqlOperator } from './OtherSetter/SqlOperator';
import TreeTypeSetter from './ValuesSetter/TreeTypeSetter';
import { ValuesSetter } from './ValuesSetter/ValuesSetter';

export const ControllerValuesName = ['config', 'controllerValues'];
export const DateName = ['config', 'controllerDate'];
export const SqlOperatorName = ['config', 'sqlOperator'];
export const PickerTypeName = [...DateName, 'pickerType'];
export const StartTimeName = [...DateName, 'startTime'];
export const StartTimeROEName = [...StartTimeName, 'relativeOrExact'];
export const StartTimeRelativeName = [...StartTimeName, 'relativeValue'];
export const StartTimeExactName = [...StartTimeName, 'exactValue'];
export const StartTimeDirectionName = [...StartTimeRelativeName, 'direction'];
export const StartTimeUnitName = [...StartTimeRelativeName, 'unit'];
export const StartTimeAmountName = [...StartTimeRelativeName, 'amount'];

export const EndTimeName = [...DateName, 'endTime'];
export const EndTimeROEName = [...EndTimeName, 'relativeOrExact'];
export const EndTimeRelativeName = [...EndTimeName, 'relativeValue'];
export const EndTimeExactName = [...EndTimeName, 'exactValue'];
export const EndTimeDirectionName = [...EndTimeRelativeName, 'direction'];
export const EndTimeUnitName = [...EndTimeRelativeName, 'unit'];
export const EndTimeAmountName = [...EndTimeRelativeName, 'amount'];

export const ValueOptionsName = ['config', 'valueOptions'];
export const MaxValueName = ['config', 'maxValue'];
export const MinValueName = ['config', 'minValue'];

export const RadioButtonTypeName = ['config', 'radioButtonType'];
export const SliderStepName = ['config', 'sliderConfig', 'step'];
export const SliderShowMarksName = ['config', 'sliderConfig', 'showMarks'];

export const CascadesName = ['config', 'cascades'];
export interface RelatedViewFormProps {
  controllerType: ControllerFacadeTypes;
  form: FormInstance<ControllerWidgetContent> | undefined;
  viewMap: Record<string, ChartDataView>;
  otherStrFilterWidgets: Widget[];
  boardType: BoardType;
  boardVizs?: Widget[];
  wid?: string;
}

export const WidgetControlForm: React.FC<RelatedViewFormProps> = memo(
  ({
    controllerType,
    form,
    viewMap,
    otherStrFilterWidgets,
    boardVizs,
    wid,
  }) => {
    const t = useI18NPrefix(`viz.board.setting`);
    const tc = useI18NPrefix('viz.control');
    const tgb = useI18NPrefix('global.button');
    const hasRadio = useMemo(() => {
      return controllerType === ControllerFacadeTypes.RadioGroup;
    }, [controllerType]);

    const isTree = useMemo(() => {
      return controllerType === ControllerFacadeTypes.DropDownTree;
    }, [controllerType]);

    const boardAllWidgetNames = useMemo(() => {
      return (boardVizs || [])
        .filter(bvz => bvz?.id !== wid)
        .map(bvz => bvz?.config?.name)
        .filter(Boolean);
    }, [boardVizs, wid]);

    const sliderTypes = useMemo(() => {
      const sliderTypes = [
        ControllerFacadeTypes.Slider,
        ControllerFacadeTypes.RangeSlider,
      ];
      return sliderTypes.includes(controllerType);
    }, [controllerType]);

    return (
      <Wrapper>
        <Form.Item
          name="name"
          label={tc('title')}
          rules={[
            {
              required: true,
              message: t('requiredWidgetName'),
            },
            () => ({
              validator(_, value) {
                if (
                  value &&
                  boardAllWidgetNames?.some(name => name === value)
                ) {
                  return Promise.reject(new Error(t('duplicateWidgetName')));
                } else {
                  return Promise.resolve();
                }
              },
            }),
          ]}
        >
          <Input />
        </Form.Item>
        {isTree && <TreeTypeSetter form={form} />}

        <ValuesSetter
          controllerType={controllerType}
          form={form}
          viewMap={viewMap}
        />
        {/* slider */}
        {sliderTypes && (
          <>
            <SliderStep label={tc('step')} />
            <SliderMarks
              label={tc('showMark')}
              switchTexts={[tgb('open'), tgb('close')]}
            />
          </>
        )}
        {/* sql 对应关系 */}
        <SqlOperator controllerType={controllerType} />

        {/* 按钮样式 */}
        {hasRadio && <RadioStyleForm />}

        {/* 是否显示 */}
        <ControllerVisibility
          otherStrFilterWidgets={otherStrFilterWidgets}
          form={form}
        />
      </Wrapper>
    );
  },
);
const Wrapper = styled.div`
  display: flex;
  flex-direction: column;
  overflow-y: auto;

  .hide-item {
    display: none;
  }
`;

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

import { Form } from 'antd';
import { ControllerFacadeTypes, TimeFilterValueCategory } from 'app/constants';
import { ControllerWidgetContent } from 'app/pages/DashBoardPage/pages/Board/slice/types';
import {
  ControllerConfig,
  ControllerDate,
  ControlOption,
} from 'app/pages/DashBoardPage/pages/BoardEditor/components/ControllerWidgetPanel/types';
import { Widget } from 'app/pages/DashBoardPage/types/widgetTypes';
import { getControllerDateValues } from 'app/pages/DashBoardPage/utils';
import { RelationFilterValue } from 'app/types/ChartConfig';
import produce from 'immer';
import React, { memo, useCallback, useContext, useMemo } from 'react';
import styled from 'styled-components/macro';
import { isEmpty } from 'utils/object';
import { convertToTree } from '../../../utils/widget';
import { WidgetActionContext } from '../../ActionProvider/WidgetActionProvider';
import { WidgetTitle } from '../../WidgetComponents/WidgetTitle';
import { getWidgetTitle } from '../../WidgetManager/utils/utils';
import { WidgetDataContext } from '../../WidgetProvider/WidgetDataProvider';
import { WidgetContext } from '../../WidgetProvider/WidgetProvider';
import { CheckboxGroupControllerForm } from './Controller/CheckboxGroupController';
import { MultiSelectControllerForm } from './Controller/MultiSelectController';
import { NumberControllerForm } from './Controller/NumberController';
import { RadioGroupControllerForm } from './Controller/RadioGroupController';
import { RangeNumberControllerForm } from './Controller/RangeNumberController';
import { RangeTimeControllerForm } from './Controller/RangeTimeController';
import { SelectControllerForm } from './Controller/SelectController';
import { SlideControllerForm } from './Controller/SliderController';
import { TextControllerForm } from './Controller/TextController';
import { TimeControllerForm } from './Controller/TimeController';
import { TreeControllerForm } from './Controller/TreeController';

export const ControllerWidgetCore: React.FC<{}> = memo(() => {
  const widget = useContext(WidgetContext);
  const content = widget.config.content as ControllerWidgetContent;
  const { onUpdateWidgetConfigByKey, onRefreshWidgetsByController } =
    useContext(WidgetActionContext);
  // TODO 由控制器自己控制 要不要触发查询
  // const emitQuery = getControlQueryEnable(widget.config.customConfig.props!);
  const [form] = Form.useForm();

  const { data: dataset } = useContext(WidgetDataContext);

  const refreshLinkedWidgets = useCallback(
    (widget: Widget) => {
      // if (!emitQuery) return;
      onRefreshWidgetsByController(widget);
    },
    [onRefreshWidgetsByController],
  );
  const { config, type: facadeType } = useMemo(() => content, [content]);

  const {
    controllerDate,
    controllerValues,
    valueOptions,
    valueOptionType,
    parentFields,
    buildingMethod,
    // sqlOperator,
  } = useMemo(() => config as ControllerConfig, [config]);
  const title = getWidgetTitle(widget.config.customConfig.props);
  title.title = widget.config.name;
  const leftControlLabel = useMemo(() => {
    if (!title.showTitle) {
      return null;
    }
    if (title.textAlign === 'center') {
      return null;
    }
    return <WidgetTitle title={title} />;
  }, [title]);
  const centerControlLabel = useMemo(() => {
    if (!title.showTitle) {
      return null;
    }
    if (title.textAlign === 'center') {
      return (
        <div style={{ width: '100%', textAlign: 'center' }}>
          <WidgetTitle title={title} />
        </div>
      );
    }
    return null;
  }, [title]);
  const optionRows = useMemo(() => {
    const dataRows = dataset?.rows || [];

    if (valueOptionType === 'common') {
      if (parentFields?.length) {
        return convertToTree(dataRows, buildingMethod);
      }
      return dataRows.map(ele => {
        const item: RelationFilterValue = {
          key: ele?.[0],
          label: ele?.[1] ?? ele?.[0],
          // children?
        };
        return item;
      });
    } else if (valueOptionType === 'custom') {
      return valueOptions;
    } else {
      return [];
    }
  }, [
    dataset?.rows,
    valueOptionType,
    valueOptions,
    parentFields,
    buildingMethod,
  ]);

  const onControllerChange = useCallback(() => {
    form.submit();
  }, [form]);

  const onFinish = value => {
    const values = value.value;
    if (values && typeof values === 'object' && !Array.isArray(values)) {
      return;
    }
    const _values = !isEmpty(values)
      ? Array.isArray(values)
        ? values
        : [values]
      : [];
    const nextContent = produce(content, draft => {
      draft.config.controllerValues = _values;
    });
    onUpdateWidgetConfigByKey({
      wid: widget.id,
      key: 'content',
      val: nextContent,
    });

    refreshLinkedWidgets(widget);
  };
  // const onSqlOperatorAndValues = useCallback(
  //   (sql: FilterSqlOperator, values: any[]) => {
  //     const nextWidget = produce(widget, draft => {
  //       (draft.config.content as ControllerWidgetContent).config.sqlOperator =
  //         sql;
  //       (
  //         draft.config.content as ControllerWidgetContent
  //       ).config.controllerValues = values;
  //     });
  //     widgetUpdate(nextWidget);
  //     refreshWidgetsByFilter(nextWidget);
  //   },
  //   [refreshWidgetsByFilter, widget, widgetUpdate],
  // );
  const onRangeTimeChange = useCallback(
    (timeValues: string[] | null) => {
      const nextFilterDate: ControllerDate = {
        ...controllerDate!,
        startTime: {
          relativeOrExact: TimeFilterValueCategory.Exact,
          exactValue: timeValues?.[0],
        },
        endTime: {
          relativeOrExact: TimeFilterValueCategory.Exact,
          exactValue: timeValues?.[1],
        },
      };
      const nextContent = produce(content, draft => {
        draft.config.controllerDate = nextFilterDate;
      });
      onUpdateWidgetConfigByKey({
        wid: widget.id,
        key: 'content',
        val: nextContent,
      });
      refreshLinkedWidgets(widget);
    },
    [
      controllerDate,
      content,
      onUpdateWidgetConfigByKey,
      widget,
      refreshLinkedWidgets,
    ],
  );

  const onTimeChange = useCallback(
    (value: string | null) => {
      const nextFilterDate: ControllerDate = {
        ...controllerDate!,
        startTime: {
          relativeOrExact: TimeFilterValueCategory.Exact,
          exactValue: value,
        },
      };

      const nextContent = produce(content, draft => {
        draft.config.controllerDate = nextFilterDate;
      });
      onUpdateWidgetConfigByKey({
        wid: widget.id,
        key: 'content',
        val: nextContent,
      });
      refreshLinkedWidgets(widget);
    },
    [
      controllerDate,
      content,
      onUpdateWidgetConfigByKey,
      widget,
      refreshLinkedWidgets,
    ],
  );

  const control = useMemo(() => {
    let selectOptions = optionRows?.map(ele => {
      return { value: ele.key, label: ele.label } as ControlOption;
    });
    switch (facadeType) {
      case ControllerFacadeTypes.DropdownList:
        form.setFieldsValue({ value: controllerValues?.[0] });
        return (
          <SelectControllerForm
            onChange={onControllerChange}
            options={selectOptions}
            name={'value'}
            label={leftControlLabel}
          />
        );

      case ControllerFacadeTypes.MultiDropdownList:
        form.setFieldsValue({ value: controllerValues });
        return (
          <MultiSelectControllerForm
            onChange={onControllerChange}
            options={selectOptions}
            name={'value'}
            label={leftControlLabel}
          />
        );
      case ControllerFacadeTypes.CheckboxGroup:
        form.setFieldsValue({ value: controllerValues });
        return (
          <CheckboxGroupControllerForm
            onChange={onControllerChange}
            options={selectOptions}
            name={'value'}
            label={leftControlLabel}
          />
        );
      case ControllerFacadeTypes.Slider:
        form.setFieldsValue({ value: controllerValues?.[0] });
        const step = config.sliderConfig?.step || 1;
        const showMarks = config.sliderConfig?.showMarks || false;
        let minValue = config.minValue === 0 ? 0 : config.minValue || 1;
        let maxValue = config.maxValue === 0 ? 0 : config.maxValue || 100;
        return (
          <SlideControllerForm
            onChange={onControllerChange}
            minValue={minValue}
            maxValue={maxValue}
            step={step}
            name="value"
            showMarks={showMarks}
            label={leftControlLabel}
          />
        );

      case ControllerFacadeTypes.Value:
        form.setFieldsValue({ value: controllerValues?.[0] });
        return (
          <NumberControllerForm
            onChange={onControllerChange}
            name="value"
            label={leftControlLabel}
          />
        );

      case ControllerFacadeTypes.RangeValue:
        form.setFieldsValue({ value: controllerValues });
        return (
          <RangeNumberControllerForm
            onChange={onControllerChange}
            name="value"
            label={leftControlLabel}
          />
        );

      case ControllerFacadeTypes.Text:
        form.setFieldsValue({ value: controllerValues?.[0] });
        return (
          <TextControllerForm
            onChange={onControllerChange}
            label={leftControlLabel}
            name="value"
          />
        );

      case ControllerFacadeTypes.RadioGroup:
        form.setFieldsValue({ value: controllerValues?.[0] });
        let RadioOptions = optionRows?.map(ele => {
          return { value: ele.key, label: ele.label } as ControlOption;
        });
        let radioButtonType = config.radioButtonType;
        return (
          <RadioGroupControllerForm
            label={leftControlLabel}
            radioButtonType={radioButtonType}
            onChange={onControllerChange}
            options={RadioOptions}
            name="value"
          />
        );

      case ControllerFacadeTypes.RangeTime:
        const rangeTimeValues = getControllerDateValues({
          controlType: facadeType,
          filterDate: config!.controllerDate!,
        });

        form.setFieldsValue({ value: rangeTimeValues });
        let rangePickerType = controllerDate!.pickerType;
        return (
          <RangeTimeControllerForm
            label={leftControlLabel}
            pickerType={rangePickerType}
            onChange={onRangeTimeChange}
            name="value"
          />
        );

      case ControllerFacadeTypes.Time:
        const timeValues = getControllerDateValues({
          controlType: facadeType,
          filterDate: config!.controllerDate!,
        });
        let pickerType = controllerDate!.pickerType;
        form.setFieldsValue({ value: timeValues[0] });
        return (
          <TimeControllerForm
            label={leftControlLabel}
            onChange={onTimeChange}
            pickerType={pickerType}
            name="value"
          />
        );

      case ControllerFacadeTypes.DropDownTree:
        form.setFieldsValue({ value: controllerValues });
        return (
          <TreeControllerForm
            parentFields={parentFields}
            onChange={onControllerChange}
            treeData={optionRows}
            name={'value'}
            label={leftControlLabel}
          />
        );

      default:
        break;
    }
  }, [
    optionRows,
    facadeType,
    form,
    controllerValues,
    onControllerChange,
    leftControlLabel,
    config,
    controllerDate,
    parentFields,
    onRangeTimeChange,
    onTimeChange,
  ]);

  return (
    <Wrapper formFrontColor={title?.font?.color}>
      <Form form={form} className="control-form" onFinish={onFinish}>
        {centerControlLabel}
        {control}
      </Form>
    </Wrapper>
  );
});
const Wrapper = styled.div<{ formFrontColor: string }>`
  display: flex;
  flex: 1;
  align-items: center;
  .control-form {
    flex: 1;

    .ant-select,
    .ant-checkbox-wrapper,
    .ant-input,
    .ant-input-number,
    .ant-slider,
    .ant-picker input,
    .ant-radio-wrapper {
      color: ${p => p.formFrontColor} !important;
    }
  }
  .ant-form-item {
    margin-bottom: 0;
  }
  .ant-input-number-handler {
    border-left: 0;
  }
`;

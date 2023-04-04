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
import { Form, FormInstance, Select } from 'antd';
import { ControllerFacadeTypes, TimeFilterValueCategory } from 'app/constants';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import { ControllerWidgetContent } from 'app/pages/DashBoardPage/pages/Board/slice/types';
import produce from 'immer';
import React, { useCallback } from 'react';
import {
  DateName,
  EndTimeExactName,
  EndTimeName,
  EndTimeRelativeName,
  EndTimeROEName,
  PickerTypeName,
  StartTimeExactName,
  StartTimeRelativeName,
  StartTimeROEName,
} from '../..';
import {
  ControllerConfig,
  ControllerDateType,
  PickerTypeOptions,
  RelativeDate,
} from '../../../types';
import { RelativeTimeSetter } from './RelativeTimeSetter';
import { SingleTimeSet } from './SingleTimeSet';

export const TimeSetter: React.FC<{
  form: FormInstance<ControllerWidgetContent> | undefined;
  controllerType: ControllerFacadeTypes;
}> = ({ controllerType, form }) => {
  const tc = useI18NPrefix(`viz.control`);
  const tvt = useI18NPrefix(`viz.tips`);
  const filterDataT = useI18NPrefix('viz.common.filter.date');
  const getControllerConfig = useCallback(() => {
    return form?.getFieldValue('config') as ControllerConfig;
  }, [form]);

  const getStartRelativeOrExact = useCallback(() => {
    const config = getControllerConfig();
    return config?.controllerDate?.startTime.relativeOrExact;
  }, [getControllerConfig]);

  const getEndRelativeOrExact = useCallback(() => {
    const config = getControllerConfig();
    return config?.controllerDate?.endTime?.relativeOrExact;
  }, [getControllerConfig]);

  const getPickerType = useCallback(() => {
    return getControllerConfig()?.controllerDate?.pickerType;
  }, [getControllerConfig]);

  const RangeTimeValidator = async (_, value) => {
    const controllerDate = getControllerConfig().controllerDate;
    if (controllerType === ControllerFacadeTypes.Time) {
      return controllerDate;
    }
    function hasValue(date: ControllerDateType | undefined) {
      if (!date) {
        return false;
      }
      if (date.relativeOrExact === TimeFilterValueCategory.Exact) {
        return date.exactValue;
      } else {
        return date.relativeValue;
      }
    }

    const startHasValue = hasValue(controllerDate?.startTime);
    const endHasValue = hasValue(controllerDate?.endTime);
    if (!startHasValue && !endHasValue) {
      return Promise.resolve(controllerDate);
    }

    if (!startHasValue && endHasValue) {
      return Promise.reject(new Error(tvt('noStartValue')));
    }
    if (startHasValue && !endHasValue) {
      return Promise.reject(new Error(tvt('noEndValue')));
    }
    return Promise.resolve(value);
  };
  const onStartRelativeChange = useCallback(
    value => {
      const valueType: TimeFilterValueCategory = value;
      if (valueType === TimeFilterValueCategory.Relative) {
        const startTime = getControllerConfig()?.controllerDate?.startTime;
        if (startTime?.relativeValue) {
        } else {
          const relativeValue: RelativeDate = {
            amount: 1,
            unit: 'd',
            direction: '-',
          };
          const newControllerDate = produce(
            getControllerConfig()!.controllerDate,
            draft => {
              draft!.startTime.relativeValue = relativeValue;
            },
          );
          form?.setFieldsValue({
            config: {
              ...getControllerConfig(),
              controllerDate: { ...newControllerDate! },
            },
          });
        }
      }
      form?.validateFields(DateName);
    },
    [form, getControllerConfig],
  );

  const onEndRelativeChange = useCallback(
    value => {
      const valueType: TimeFilterValueCategory = value;
      if (valueType === TimeFilterValueCategory.Relative) {
        const endTime = getControllerConfig()?.controllerDate?.endTime;
        if (endTime?.relativeValue) {
        } else {
          const relativeValue: RelativeDate = {
            amount: 1,
            unit: 'd',
            direction: '-',
          };
          const newControllerDate = produce(
            getControllerConfig()!.controllerDate,
            draft => {
              draft!.endTime!.relativeValue = relativeValue;
            },
          );
          form?.setFieldsValue({
            config: {
              ...getControllerConfig(),
              controllerDate: { ...newControllerDate! },
            },
          });
        }
      }
      form?.validateFields(DateName);
    },
    [form, getControllerConfig],
  );
  const renderROE = useCallback(
    (name, onChange: (value: any) => void) => {
      return (
        <Form.Item
          name={name}
          noStyle
          shouldUpdate
          validateTrigger={['onBlur']}
          rules={[{ required: true }]}
        >
          <Select style={{ width: '100px' }} onChange={onChange}>
            <Select.Option
              key={TimeFilterValueCategory.Exact}
              value={TimeFilterValueCategory.Exact}
            >
              {filterDataT('exact')}
            </Select.Option>
            <Select.Option
              key={TimeFilterValueCategory.Relative}
              value={TimeFilterValueCategory.Relative}
            >
              {filterDataT('relative')}
            </Select.Option>
          </Select>
        </Form.Item>
      );
    },
    [filterDataT],
  );
  const renderExact = useCallback((name, getPickerType: () => any) => {
    return (
      <Form.Item
        noStyle
        name={name}
        shouldUpdate
        validateTrigger={['onChange', 'onBlur']}
        // rules={[{ required: false, validator: RangeTimeValidator }]}
      >
        <SingleTimeSet pickerType={getPickerType()!} />
      </Form.Item>
    );
  }, []);

  return (
    <Form.Item noStyle shouldUpdate>
      {() => {
        return (
          <>
            <Form.Item
              name={PickerTypeName}
              label={tc('dateType')}
              shouldUpdate
              validateTrigger={['onChange', 'onBlur']}
              rules={[{ required: true }]}
            >
              <Select>
                {PickerTypeOptions.map(item => {
                  return (
                    <Select.Option key={item.value} value={item.value}>
                      {item.name}
                    </Select.Option>
                  );
                })}
              </Select>
            </Form.Item>
            {controllerType === ControllerFacadeTypes.Time && (
              <>
                <Form.Item
                  label={tc('defaultValue')}
                  shouldUpdate
                  rules={[{ required: false }]}
                >
                  {renderROE(StartTimeROEName, onStartRelativeChange)}

                  {getStartRelativeOrExact() ===
                    TimeFilterValueCategory.Exact &&
                    renderExact(StartTimeExactName, getPickerType)}

                  {getStartRelativeOrExact() ===
                    TimeFilterValueCategory.Relative && (
                    <RelativeTimeSetter relativeName={StartTimeRelativeName} />
                  )}
                </Form.Item>
              </>
            )}
            {controllerType === ControllerFacadeTypes.RangeTime && (
              <Form.Item
                name={DateName}
                validateTrigger={['onChange', 'onBlur']}
                shouldUpdate
                rules={[{ required: true, validator: RangeTimeValidator }]}
                style={{ marginBottom: 0 }}
              >
                <Form.Item label={filterDataT('startTime')} shouldUpdate>
                  {renderROE(StartTimeROEName, onStartRelativeChange)}
                  {getStartRelativeOrExact() ===
                    TimeFilterValueCategory.Exact &&
                    renderExact(StartTimeExactName, getPickerType)}
                  {getStartRelativeOrExact() ===
                    TimeFilterValueCategory.Relative && (
                    <RelativeTimeSetter relativeName={StartTimeRelativeName} />
                  )}
                </Form.Item>
                <Form.Item
                  label={filterDataT('endTime')}
                  name={EndTimeName}
                  shouldUpdate
                >
                  {renderROE(EndTimeROEName, onEndRelativeChange)}

                  {getEndRelativeOrExact() === TimeFilterValueCategory.Exact &&
                    renderExact(EndTimeExactName, getPickerType)}

                  {getEndRelativeOrExact() ===
                    TimeFilterValueCategory.Relative && (
                    <RelativeTimeSetter relativeName={EndTimeRelativeName} />
                  )}
                </Form.Item>
              </Form.Item>
            )}
          </>
        );
      }}
    </Form.Item>
  );
};

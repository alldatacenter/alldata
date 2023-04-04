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
import {
  Empty,
  Form,
  FormInstance,
  Radio,
  RadioChangeEvent,
  Select,
} from 'antd';
import {
  ChartDataViewFieldCategory,
  ControllerFacadeTypes,
} from 'app/constants';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import {
  ControllerWidgetContent,
  RelatedView,
} from 'app/pages/DashBoardPage/pages/Board/slice/types';
import { Variable } from 'app/pages/MainPage/pages/VariablePage/slice/types';
import ChartDataView from 'app/types/ChartDataView';
import { hasAggregationFunction } from 'app/utils/chartHelper';
import React, { memo, useCallback } from 'react';
import styled from 'styled-components/macro';
import { filterValueTypeByControl, isRangeTypeController } from './utils';

export interface RelatedViewFormProps {
  viewMap: Record<string, ChartDataView>;
  form: FormInstance<ControllerWidgetContent> | undefined;
  controllerType: ControllerFacadeTypes;
  queryVariables: Variable[];

  getFormRelatedViews: () => RelatedView[];
}
const Option = Select.Option;
const RadioButton = Radio.Button;
const RadioGroup = Radio.Group;
export const RelatedViewForm: React.FC<RelatedViewFormProps> = memo(
  ({ viewMap, form, queryVariables, controllerType, getFormRelatedViews }) => {
    const t = useI18NPrefix(`viz.associate`);
    const isMultiple = useCallback(
      index => {
        const relatedViews = getFormRelatedViews();
        const isVariable =
          relatedViews[index].relatedCategory ===
          ChartDataViewFieldCategory.Variable;
        const isRange = isRangeTypeController(controllerType);
        const isMultiple = isVariable && isRange;
        return isMultiple;
      },
      [controllerType, getFormRelatedViews],
    );
    const fieldValueValidator = async (opt, fieldValue: string[]) => {
      if (!fieldValue) {
        return Promise.reject(new Error(t('noValueErr')));
      }
      if (Array.isArray(fieldValue)) {
        if (fieldValue.length !== 2) {
          return Promise.reject(new Error(t('valueErr')));
        }
      }

      return Promise.resolve(fieldValue);
    };
    const filterFieldCategoryChange = useCallback(
      (index: number) => (e: RadioChangeEvent) => {
        const relatedViews = getFormRelatedViews();
        relatedViews[index].relatedCategory = e.target.value;
        relatedViews[index].fieldValue = undefined;
        form?.setFieldsValue({ relatedViews: relatedViews });
      },
      [form, getFormRelatedViews],
    );
    const fieldValueChange = useCallback(
      (index: number) => (value, option) => {
        const relatedViews = getFormRelatedViews();

        const fieldValueType = Array.isArray(option)
          ? option[0]?.fieldvaluetype
          : option?.fieldvaluetype;

        relatedViews[index].fieldValue = value;
        relatedViews[index].fieldValueType = fieldValueType;

        form?.setFieldsValue({ relatedViews: relatedViews });
      },
      [getFormRelatedViews, form],
    );

    const renderOptions = useCallback(
      (index: number) => {
        const relatedViews = getFormRelatedViews();
        if (!relatedViews) {
          return null;
        }

        if (
          relatedViews[index].relatedCategory ===
          ChartDataViewFieldCategory.Variable
        ) {
          // 变量
          return queryVariables
            .filter(v => {
              return v.viewId === relatedViews[index].viewId || !v.viewId;
            })
            .filter(v => {
              return filterValueTypeByControl(controllerType, v.valueType);
            })
            .map(item => (
              <Option
                key={item.id}
                fieldvaluetype={item.valueType}
                value={item.name}
              >
                <div
                  style={{ display: 'flex', justifyContent: 'space-between' }}
                >
                  <span>{item.name}</span>
                  <FieldType>{item.valueType}</FieldType>
                </div>
              </Option>
            ));
        } else {
          let viewComputedField =
            viewMap?.[relatedViews[index].viewId]?.computedFields?.filter(
              field =>
                !hasAggregationFunction(field?.expression) &&
                field.isViewComputedFields,
            ) || [];

          // 字段
          return viewMap?.[relatedViews[index].viewId]?.meta
            ?.concat(viewComputedField)
            ?.filter(v => {
              return filterValueTypeByControl(controllerType, v.type);
            })
            .map(item => {
              return (
                <Option
                  key={item.name}
                  fieldvaluetype={item.type}
                  value={item.name}
                >
                  <div
                    style={{ display: 'flex', justifyContent: 'space-between' }}
                  >
                    <span>{item.name}</span>
                    <FieldType>{item.type}</FieldType>
                  </div>
                </Option>
              );
            });
        }
      },
      [controllerType, getFormRelatedViews, queryVariables, viewMap],
    );

    const getViewName = useCallback(
      (index: number) => {
        const relatedViews = getFormRelatedViews();
        const name = viewMap[relatedViews[index]?.viewId]?.name || '';
        return name;
      },
      [getFormRelatedViews, viewMap],
    );

    return (
      <Wrapper>
        <h3>{t('title')}</h3>
        <Form.List
          name="relatedViews"
          rules={[
            {
              validator: async (_, relatedViews: RelatedView[]) => {
                return Promise.resolve(relatedViews);
              },
            },
          ]}
        >
          {(fields, _, { errors }) => {
            return (
              <>
                {fields.map((field, index) => (
                  <Form.Item noStyle key={index} shouldUpdate>
                    <div className="relatedView">
                      <h4>{getViewName(index)}</h4>
                      <div style={{ width: '140px', textAlign: 'right' }}>
                        <Form.Item
                          {...field}
                          validateTrigger={['onChange', 'onClick', 'onBlur']}
                          name={[field.name, 'relatedCategory']}
                          fieldKey={[field.fieldKey, 'id']}
                        >
                          <RadioGroup
                            value
                            size="small"
                            onChange={filterFieldCategoryChange(index)}
                          >
                            <RadioButton
                              value={ChartDataViewFieldCategory.Field}
                            >
                              {t('field')}
                            </RadioButton>
                            <RadioButton
                              value={ChartDataViewFieldCategory.Variable}
                            >
                              {t('variable')}
                            </RadioButton>
                          </RadioGroup>
                        </Form.Item>
                      </div>
                    </div>

                    <Form.Item
                      {...field}
                      shouldUpdate
                      validateTrigger={['onChange', 'onClick', 'onBlur']}
                      name={[field.name, 'fieldValue']}
                      fieldKey={[field.fieldKey, 'id']}
                      wrapperCol={{ span: 24 }}
                      rules={[{ validator: fieldValueValidator }]}
                    >
                      <Select
                        showSearch
                        placeholder="请选择"
                        allowClear
                        {...(isMultiple(index) && { mode: 'multiple' })}
                        onChange={fieldValueChange(index)}
                      >
                        {renderOptions(index)}
                      </Select>
                    </Form.Item>
                  </Form.Item>
                ))}
                <Form.Item>
                  <Form.ErrorList errors={errors} />
                </Form.Item>
                {!fields.length && <Empty key="empty" />}
              </>
            );
          }}
        </Form.List>
      </Wrapper>
    );
  },
);
const Wrapper = styled.div`
  display: block;
  min-height: 150px;

  overflow-y: auto;
  .relatedView {
    display: flex;
    height: 40px;
    h4 {
      flex: 1;
    }
    .fieldType {
      flex-shrink: 0;
    }
  }
`;

const FieldType = styled.span`
  color: ${p => p.theme.textColorDisabled};
`;

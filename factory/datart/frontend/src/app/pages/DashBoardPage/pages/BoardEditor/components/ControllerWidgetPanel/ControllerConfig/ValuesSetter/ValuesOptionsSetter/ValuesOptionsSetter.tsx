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
  Form,
  FormInstance,
  Radio,
  Select,
  Space,
  Tree,
  TreeSelect,
} from 'antd';
import { CascaderOptionType } from 'antd/lib/cascader';
import { ControllerFacadeTypes } from 'app/constants';
import useI18NPrefix from 'app/hooks/useI18NPrefix';
import migrationViewConfig from 'app/migration/ViewConfig/migrationViewConfig';
import beginViewModelMigration from 'app/migration/ViewConfig/migrationViewModelConfig';
import { BoardContext } from 'app/pages/DashBoardPage/components/BoardProvider/BoardProvider';
import {
  OPERATOR_TYPE_OPTION,
  ValueOptionType,
} from 'app/pages/DashBoardPage/constants';
import { ViewSimple } from 'app/pages/MainPage/pages/ViewPage/slice/types';
import { RelationFilterValue } from 'app/types/ChartConfig';
import ChartDataView from 'app/types/ChartDataView';
import { View } from 'app/types/View';
import { hasAggregationFunction } from 'app/utils/chartHelper';
import { getDistinctFields } from 'app/utils/fetch';
import { transformMeta } from 'app/utils/internalChartHelper';
import {
  FC,
  memo,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import styled from 'styled-components/macro';
import { request2 } from 'utils/request';
import { convertToTree } from '../../../../../../../utils/widget';
import { ControllerConfig } from '../../../types';
import TreeSetter from '../TreeSetter';
import { AssistViewFields } from './AssistViewFields';
import { CustomOptions } from './CustomOptions';

export interface optionProps {
  label: string;
  value: string;
}

const ValuesOptionsSetter: FC<{
  controllerType: ControllerFacadeTypes;
  form: FormInstance<{ config: ControllerConfig }> | undefined;
  viewMap: Record<string, ChartDataView>;
}> = memo(({ form, viewMap, controllerType }) => {
  const tc = useI18NPrefix(`viz.control`);
  const { orgId } = useContext(BoardContext);
  const [optionValues, setOptionValues] = useState<RelationFilterValue[]>([]);
  const [targetKeys, setTargetKeys] = useState<string[]>([]);
  const [labelOptions, setLabelOptions] = useState<
    CascaderOptionType[] | undefined
  >([]);
  const [labelKey, setLabelKey] = useState<string | undefined>();
  const [viewList, setViewList] = useState<optionProps[]>([]);

  const isTree = useMemo(() => {
    return controllerType === ControllerFacadeTypes.DropDownTree;
  }, [controllerType]);

  const getViewList = useCallback(async orgId => {
    const { data } = await request2<ViewSimple[]>(`/views?orgId=${orgId}`);
    const views = data.map(item => {
      return {
        value: item.id,
        label: item.name,
      };
    });
    setViewList(views);
  }, []);

  const getControllerConfig = useCallback(() => {
    return form?.getFieldValue('config') as ControllerConfig;
  }, [form]);

  const getParentFields = useCallback(() => {
    return form?.getFieldValue('config')?.parentFields as string[];
  }, [form]);

  const getTreeBuildingMethod = useCallback(() => {
    return form?.getFieldValue('config')?.buildingMethod as string;
  }, [form]);

  const isMultiple = useMemo(() => {
    return (
      controllerType === ControllerFacadeTypes.MultiDropdownList ||
      controllerType === ControllerFacadeTypes.DropDownTree
    );
  }, [controllerType]);

  const convertToList = useCallback(collection => {
    return collection.map((ele, index) => {
      const item: RelationFilterValue = {
        index: index,
        key: ele?.[0],
        label: ele?.[1],
        isSelected: false,
      };
      return item;
    });
  }, []);

  const getViewOption = useCallback(async (viewId: string) => {
    if (!viewId) return { option: [], dataView: undefined };
    let { data } = await request2<View>(`/views/${viewId}`);
    if (!data) {
      return { option: [], data: undefined };
    }
    if (data) {
      data = migrationViewConfig(data);
    }
    if (data?.model) {
      data.model = beginViewModelMigration(data.model, data.type);
    }
    let meta = transformMeta(data?.model);
    const viewComputedField =
      JSON.parse(data.model || '{}')?.computedFields?.filter(
        field => !hasAggregationFunction(field?.expression),
      ) || [];

    if (!meta) return { option: [], dataView: undefined };
    const option: CascaderOptionType[] = meta
      .concat(viewComputedField)
      .map(item => {
        return {
          value: item.name,
          label: item.name,
        };
      });

    return {
      option,
      dataView: { ...data, meta, computedFields: viewComputedField },
    };
  }, []);

  const onTargetKeyChange = useCallback(
    nextTargetKeys => {
      const config: ControllerConfig = getControllerConfig();

      form?.setFieldsValue({
        config: {
          ...config,
          controllerValues: nextTargetKeys,
        },
      });
      setTargetKeys(nextTargetKeys);
    },
    [form, getControllerConfig],
  );

  const fetchNewDataset = useCallback(
    async (viewId: string, columns: string[], dataView?: ChartDataView) => {
      const fieldDataset = await getDistinctFields(
        viewId,
        columns,
        dataView,
        undefined,
      );
      return fieldDataset;
    },
    [],
  );

  const getViewData = useCallback(
    async (value: string[], type?) => {
      const { option: options, dataView } = await getViewOption(value[0]);
      setLabelOptions(options);

      if (type !== 'view') {
        const [viewId, ...columns] = value;
        const parentFields = getParentFields();
        const buildingMethod = getTreeBuildingMethod();

        if (parentFields) {
          columns.push(...parentFields);
        }
        const dataset = await fetchNewDataset(viewId, columns, dataView);

        setOptionValues(
          parentFields?.length > 0
            ? convertToTree(dataset?.rows, buildingMethod)
            : convertToList(dataset?.rows),
        );
      }
    },
    [
      convertToList,
      fetchNewDataset,
      getParentFields,
      getViewOption,
      getTreeBuildingMethod,
    ],
  );

  const onViewFieldChange = useCallback(
    async (value: string[], type?) => {
      setOptionValues([]);
      setTargetKeys([]);
      setLabelOptions([]);
      setLabelKey(undefined);
      const config = getControllerConfig();
      if (!value || !value?.[0]) {
        form?.setFieldsValue({
          config: {
            ...config,
            assistViewFields: [],
            controllerValues: [],
            parentFields: undefined,
          },
        });
        return;
      }

      form?.setFieldsValue({
        config: {
          ...config,
          assistViewFields: value,
          controllerValues: [],
          parentFields: undefined,
        },
      });
      getViewData(value, type);
    },
    [form, getControllerConfig, getViewData],
  );

  const onLabelChange = useCallback(
    (labelKey: string | undefined) => {
      const controllerConfig = getControllerConfig();
      const [viewId, valueId] = controllerConfig.assistViewFields || [];
      setLabelKey(labelKey);
      const nextAssistViewFields = labelKey
        ? [viewId, valueId, labelKey]
        : [viewId, valueId];
      const nextControllerOpt: ControllerConfig = {
        ...controllerConfig,
        assistViewFields: nextAssistViewFields,
      };
      form?.setFieldsValue({
        config: nextControllerOpt,
      });
      getViewData(nextAssistViewFields);
    },
    [form, getControllerConfig, getViewData],
  );

  const onInitOptions = useCallback(
    async (value: string[], parentFields?: string[]) => {
      const [viewId, ...columns] = value;
      const { option: options, dataView } = await getViewOption(viewId);
      if (parentFields) {
        columns.push(...parentFields);
      }
      const dataset = await fetchNewDataset(viewId, columns, dataView);
      const config: ControllerConfig = getControllerConfig();
      const buildingMethod = getTreeBuildingMethod();

      setOptionValues(
        parentFields
          ? convertToTree(dataset?.rows, buildingMethod)
          : convertToList(dataset?.rows),
      );
      setLabelOptions(options);

      if (config.valueOptionType === 'common') {
        setLabelKey(config.assistViewFields?.[2]);
        if (config?.controllerValues) {
          setTargetKeys(config?.controllerValues);
        }
      }
    },
    [
      convertToList,
      fetchNewDataset,
      getControllerConfig,
      getViewOption,
      getTreeBuildingMethod,
    ],
  );

  const updateOptions = useCallback(() => {
    const config = getControllerConfig();
    if (!config?.valueOptionType) {
      form?.setFieldsValue({
        config: { ...config, valueOptionType: 'common' },
      });
    }

    const assistViewFields = config?.assistViewFields;
    const parentFields = config?.parentFields;
    if (assistViewFields && assistViewFields[0] && assistViewFields[1]) {
      onInitOptions(assistViewFields, parentFields);
    }
  }, [form, getControllerConfig, onInitOptions]);

  const getOptionType = useCallback(() => {
    return getControllerConfig()?.valueOptionType as ValueOptionType;
  }, [getControllerConfig]);

  const onParentFieldsChange = useCallback(
    (val: string | string[]) => {
      let mergedConfig: ControllerConfig = {
        ...getControllerConfig(),
        parentFields: Array.isArray(val) ? val : [val],
      };

      if (getTreeBuildingMethod() === 'byHierarchy') {
        mergedConfig.assistViewFields = mergedConfig.assistViewFields!.slice(
          0,
          1,
        );

        if ((val as string[]).length) {
          mergedConfig.assistViewFields = mergedConfig.assistViewFields.concat(
            (val as string[])[0],
          );
        }
      }

      form?.setFieldsValue({ config: mergedConfig });
      getViewData(mergedConfig.assistViewFields || []);
    },
    [getControllerConfig, getTreeBuildingMethod, getViewData, form],
  );

  useEffect(() => {
    setTimeout(() => {
      updateOptions();
    }, 500);
  }, [updateOptions]);

  useEffect(() => {
    getViewList(orgId);
  }, [getViewList, orgId]);

  return (
    <Wrapper>
      <Form.Item
        label={tc('valueConfig')}
        shouldUpdate
        style={{ marginBottom: '0' }}
      >
        <Form.Item
          name={['config', 'valueOptionType']}
          validateTrigger={['onChange', 'onBlur']}
          rules={[{ required: true }]}
          style={{ marginBottom: '0' }}
          hidden={isTree}
        >
          <Radio.Group>
            {OPERATOR_TYPE_OPTION.map(ele => {
              return (
                <Radio.Button key={ele.value} value={ele.value}>
                  {tc(ele.value)}
                </Radio.Button>
              );
            })}
          </Radio.Group>
        </Form.Item>
        <Form.Item shouldUpdate>
          {() => {
            return (
              <>
                <Form.Item name={['config', 'assistViewFields']} noStyle>
                  <AssistViewFields
                    onChange={onViewFieldChange}
                    viewList={viewList}
                    viewFieldList={labelOptions}
                    isHierarchyTree={
                      isTree && getTreeBuildingMethod() === 'byHierarchy'
                    }
                    style={{ margin: '6px 0' }}
                  />
                </Form.Item>
                {isTree && (
                  <Form.Item name={['config', 'parentFields']} noStyle>
                    <TreeSetter
                      mode={
                        getTreeBuildingMethod() === 'byHierarchy'
                          ? 'multiple'
                          : undefined
                      }
                      onChange={onParentFieldsChange}
                      viewFieldList={labelOptions}
                      style={{ margin: '6px 0' }}
                    />
                  </Form.Item>
                )}

                {getOptionType() === 'common' && (
                  <Space direction="vertical" style={{ width: '100%' }}>
                    {!(isTree && getTreeBuildingMethod() === 'byHierarchy') && (
                      <Select
                        showSearch
                        placeholder={tc('optionLabelField')}
                        value={labelKey}
                        allowClear
                        onChange={onLabelChange}
                        style={{ width: '100%' }}
                      >
                        {labelOptions?.map(item => (
                          <Select.Option
                            key={item.value}
                            value={item.value as string}
                          >
                            {item.label}
                          </Select.Option>
                        ))}
                      </Select>
                    )}
                    <Form.Item name={['config', 'controllerValues']}>
                      {getParentFields()?.length ? (
                        <TreeSelect
                          placeholder={tc('selectDefaultValue')}
                          multiple
                          allowClear
                          treeData={optionValues}
                          style={{ width: '100%' }}
                          dropdownStyle={{ height: '300px', overflowY: 'auto' }}
                          dropdownRender={() => {
                            return (
                              <TreeSelectProps
                                checkedKeys={targetKeys}
                                onCheck={(checkedObj: any) =>
                                  onTargetKeyChange(checkedObj?.checked)
                                }
                                checkable
                                checkStrictly
                                titleRender={node => {
                                  return (
                                    <div
                                      style={{
                                        width: '100%',
                                        display: 'flex',
                                        justifyContent: 'space-between',
                                      }}
                                    >
                                      <span>{node.title || node.key}</span>
                                      <FieldKey>{node.key}</FieldKey>
                                    </div>
                                  );
                                }}
                                treeData={optionValues}
                              />
                            );
                          }}
                        />
                      ) : (
                        <Select
                          showSearch
                          placeholder={tc('selectDefaultValue')}
                          value={targetKeys}
                          allowClear
                          {...(isMultiple && { mode: 'multiple' })}
                          onChange={onTargetKeyChange}
                          style={{ width: '100%' }}
                        >
                          {optionValues.map(item => (
                            <Select.Option
                              key={String(item.key) + String(item.label)}
                              value={item.key}
                            >
                              <div
                                style={{
                                  display: 'flex',
                                  justifyContent: 'space-between',
                                }}
                              >
                                <span>{item.label || item.key}</span>
                                <FieldKey>{item.key}</FieldKey>
                              </div>
                            </Select.Option>
                          ))}
                        </Select>
                      )}
                    </Form.Item>
                  </Space>
                )}
                {getOptionType() === 'custom' && (
                  <CustomOptions
                    getControllerConfig={getControllerConfig}
                    form={form}
                    fieldRowData={optionValues}
                  />
                )}
              </>
            );
          }}
        </Form.Item>
      </Form.Item>
    </Wrapper>
  );
});

export default ValuesOptionsSetter;

const Wrapper = styled.div`
  .transfer {
    padding: 10px 0;
  }
`;

const FieldKey = styled.span`
  color: ${p => p.theme.textColorDisabled};
`;

const TreeSelectProps = styled(Tree)`
  .ant-tree-node-content-wrapper {
    width: 100%;
  }
  .ant-tree-treenode {
    width: 100%;
  }
`;

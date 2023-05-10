// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

import React from 'react';
import { Select, Button, Popconfirm } from 'antd';
import i18n from 'i18n';
import { Icon as CustomIcon, FormBuilder, IFormExtendType } from 'common';
import { useUpdate } from 'common/use-hooks';
import { produce } from 'immer';
import { map, set, unset, keys, isEmpty, get, filter, omit, some, forEach, reduce, values } from 'lodash';
import { regRules } from 'common/utils';
import ApiParamsModal from 'apiManagePlatform/pages/api-market/design/api-params-modal';
import {
  BASE_DATA_TYPE,
  API_PROPERTY_REQUIRED,
  API_MEDIA,
  DATATYPE_EXAMPLE_MAP,
  QUOTE_PREFIX_NO_EXTENDED,
  QUOTE_PREFIX,
  API_FORM_KEY,
} from 'app/modules/apiManagePlatform/configs.ts';
import {
  getPropertyFormSelector,
  objectExampleField,
  descriptionField,
  getPropertyDetailFields,
} from './property-field';
import './basic-params-config.scss';

const { Fields } = FormBuilder;
const { Option } = Select;

const initialTypeMap = {
  Response: 'object',
  Body: 'object',
};

type IFormType = 'Response' | 'Query' | 'Parameters' | 'DataType' | 'Body' | 'Array';
interface IPropertyItemForm {
  detailVisible?: boolean;
  isEditMode?: boolean;
  formType?: IFormType;
  formData: Obj;
  siblingProperties?: Obj[];
  extraDataTypes?: Obj;
  allExtraDataTypes?: Obj;
  allDataTypes?: string[];
  onChange: (formKey: string, e: any, extraProps?: Obj) => void;
  onSetMediaType?: ({ propertyKey, propertyData }: { propertyKey: string; propertyData: string }) => void;
  updateErrorNum?: (n: number, name: string) => void;
  onFormErrorNumChange?: (n: number, name: string) => void;
  index?: number;
}

interface ISetFieldProps {
  propertyType?: string;
  propertyKey?: string;
  propertyData?: any;
}

interface IFormErrorMap {
  [prop: string]: number;
}

export const PropertyItemForm = React.memo((props: IPropertyItemForm) => {
  const { formData, onChange, formType = 'Query', isEditMode = true, index } = props;

  const [
    {
      detailVisible,
      curPropertyType,
      innerParamList,
      dataTempStorage,
      paramListTempStorage,
      paramsModalVisible,
      arrayItemDataStorage,
      errorMap,
      formErrorMap,
    },
    updater,
    update,
  ] = useUpdate({
    detailVisible: props.detailVisible || false,
    curPropertyType: formData.type || initialTypeMap[formType] || ('string' as API_SETTING.PropertyType),
    innerParamList: [],
    dataTempStorage: {},
    paramListTempStorage: [],
    paramsModalVisible: false,
    arrayItemDataStorage: null,

    errorMap: {
      name: false,
      maxLength: false,
      minLength: false,
      maximum: false,
      minimum: false,
    },
    formErrorMap: {},
  });

  const formRef = React.useRef<IFormExtendType>({} as any);
  const paramListTempStorageRef = React.useRef<any[]>([]);
  const dataTempStorageRef = React.useRef<Obj>({});

  React.useImperativeHandle(paramListTempStorageRef, () => paramListTempStorage);
  React.useImperativeHandle(dataTempStorageRef, () => dataTempStorage);

  const isCurTypeOf = React.useCallback(
    (type: BASE_DATA_TYPE) => {
      return curPropertyType === type || get(props, ['extraDataTypes', curPropertyType, 'type']) === type;
    },
    [curPropertyType, props],
  );

  const isBasicDataType = React.useMemo(() => {
    return some(BASE_DATA_TYPE, (item) => item === curPropertyType);
  }, [curPropertyType]);

  const getRefTypePath = React.useCallback((data: Obj): string => {
    return get(data, [QUOTE_PREFIX, 0, '$ref']) || get(data, [QUOTE_PREFIX_NO_EXTENDED]) || '';
  }, []);

  const getExampleData = React.useCallback(
    (data: Obj, extraTypes?: Obj) => {
      if (!data) return '';
      const _extraTypes = extraTypes || props?.extraDataTypes;

      const refTypePath = getRefTypePath(data);
      const customType = refTypePath.split('/').slice(-1)[0];
      const customTypeData = get(_extraTypes, [customType]) || customType || {};

      if (typeof customTypeData === 'string') {
        const _type =
          get(props, ['allExtraDataTypes', customType, 'type']) || get(props, ['extraDataTypes', customType, 'type']);
        return _type === 'array' ? [] : {};
      }

      const curType = data.type || customTypeData.type;

      if (curType === 'object') {
        const newExtraTypes = produce(_extraTypes, (draft) => {
          draft && (draft[customType] = null);
        });

        const newExample: Obj = refTypePath ? getExampleData(customTypeData, newExtraTypes) : {};

        const customProperties = data.properties || {};
        forEach(keys(customProperties), (pName) => {
          const propertyItem = customProperties[pName];
          newExample[pName] = getExampleData(propertyItem, newExtraTypes);
        });

        return newExample;
      } else if (curType === 'array') {
        if (refTypePath) {
          const newItemExtraTypes = produce(_extraTypes, (draft) => {
            draft && (draft[customType] = null);
          });
          return getExampleData(customTypeData, newItemExtraTypes);
        } else {
          return [getExampleData(data.items, _extraTypes)];
        }
      } else if (refTypePath && customTypeData.example !== undefined) {
        return customTypeData.example;
      } else if (data.example !== undefined) {
        return data.example;
      } else {
        return DATATYPE_EXAMPLE_MAP[curType] || '';
      }
    },
    [getRefTypePath, props],
  );

  // 表单初始化加载
  React.useEffect(() => {
    formRef.current.resetFields();
    update({
      errorMap: {
        name: false,
        maxLength: false,
        minLength: false,
        maximum: false,
        minimum: false,
      },
      formErrorMap: {},
    });

    const innerProperties = formData?.properties;
    const requiredNames = formData?.required || [];
    updater.dataTempStorage(formData);

    const tempList = map(keys(innerProperties), (pKey: string) => {
      const _temp = { ...innerProperties[pKey] };
      if (formData?.type === 'object' && Array.isArray(formData?.required)) {
        _temp[API_PROPERTY_REQUIRED] = requiredNames.includes(pKey);
      }
      _temp[API_FORM_KEY] = pKey;

      return _temp;
    });
    updater.innerParamList(tempList);
    updater.paramListTempStorage(tempList);

    let _curPropertyType = formData?.type || formData?.schema?.type || 'object';

    const tempFormData = { ...formData };

    const refTypePath = getRefTypePath(formData);
    if (refTypePath) {
      const customType = refTypePath.split('/').slice(-1)[0];
      tempFormData.type = customType;
      _curPropertyType = customType;
    }

    updater.curPropertyType(_curPropertyType);

    setTimeout(() => {
      formRef.current!.setFieldsValue(tempFormData);
      if (isEmpty(formData)) {
        const _formData = formRef.current!.getFieldsValue();
        updater.dataTempStorage(_formData);
      } else {
        updater.dataTempStorage(formData);
      }
    });
  }, [updater, formData, getRefTypePath, update]);

  const AllDataTypes = React.useMemo(() => {
    return filter(props?.allDataTypes, (item) => item !== dataTempStorage[API_FORM_KEY]) || [];
  }, [dataTempStorage, props]);

  const onToggleDetail = React.useCallback(
    (visible) => {
      if (visible) {
        const omitList = getRefTypePath(dataTempStorage) ? ['type', API_FORM_KEY] : [API_FORM_KEY];
        const tempFormData = omit(dataTempStorage, omitList);
        const example = getExampleData(tempFormData);
        setTimeout(() => formRef.current!.setFieldsValue({ ...tempFormData, example }));
      }
      updater.dataTempStorage(dataTempStorageRef.current);
      if (curPropertyType === 'array' && arrayItemDataStorage) {
        updater.dataTempStorage(arrayItemDataStorage);
      } else {
        updater.dataTempStorage(dataTempStorageRef.current);
      }
      updater.innerParamList(paramListTempStorageRef.current);
      updater.detailVisible(visible);
    },
    [arrayItemDataStorage, curPropertyType, dataTempStorage, getExampleData, getRefTypePath, updater],
  );

  const propertyNameMap = React.useMemo(() => {
    const list = props?.siblingProperties || [];
    return map(list, (item) => item[API_FORM_KEY]);
  }, [props]);

  const setFields = React.useCallback(
    (fieldProps: ISetFieldProps) => {
      const { propertyKey = '', propertyData } = fieldProps;
      if (propertyKey === API_MEDIA && props.onSetMediaType) {
        props.onSetMediaType(fieldProps as { propertyKey: string; propertyData: string });
        return;
      }
      if (propertyKey === 'operation') {
        updater.detailVisible(propertyData);
        return;
      }
      if (formRef?.current) {
        const newFormData = produce(dataTempStorageRef.current, (draft: any) => {
          if (
            curPropertyType === 'array' &&
            !['description', 'type', API_FORM_KEY, API_PROPERTY_REQUIRED].includes(propertyKey)
          ) {
            set(draft, `items.${propertyKey}`, propertyData);
          } else {
            set(draft, propertyKey, propertyData);
          }

          if (propertyKey === 'type') {
            const curType = propertyData;
            updater.curPropertyType(curType);
            unset(draft, QUOTE_PREFIX);
            unset(draft, QUOTE_PREFIX_NO_EXTENDED);
            unset(draft, 'default');
            unset(draft, 'enum');

            if (curType === 'object' || curType === 'array') {
              unset(draft, 'pattern');
              unset(draft, 'maxLength');
              unset(draft, 'minLength');
              unset(draft, 'format');
              unset(draft, 'maximum');
              unset(draft, 'minimum');

              if (curType === 'object') {
                set(draft, 'properties', {});
                set(draft, 'required', []);
                unset(draft, 'items');
              }
              if (curType === 'array') {
                const tempItemData = {
                  type: 'string',
                  example: 'Example',
                };
                tempItemData[API_FORM_KEY] = 'items';
                set(draft, 'items', tempItemData);
                unset(draft, 'properties');
                updater.innerParamList([]);
                updater.paramListTempStorage([]);
              }
            } else if (['boolean', 'string', 'number', 'integer'].includes(curType)) {
              unset(draft, 'items');
              unset(draft, 'properties');
              unset(draft, 'required');
              if (curType !== 'number') {
                unset(draft, 'format');
                unset(draft, 'maximum');
                unset(draft, 'minimum');
              }
              if (curType !== 'string') {
                unset(draft, 'pattern');
                unset(draft, 'maxLength');
                unset(draft, 'minLength');
              }
              updater.innerParamList([]);
              updater.paramListTempStorage([]);
            }
            set(draft, 'example', DATATYPE_EXAMPLE_MAP[curType]);
          }
        });

        if (propertyKey === 'type') {
          if (!DATATYPE_EXAMPLE_MAP[propertyData]) {
            const customTypeData = get(props, ['extraDataTypes', propertyData]) || {};
            const _newTypeData = {
              ...omit(dataTempStorage, [QUOTE_PREFIX, QUOTE_PREFIX_NO_EXTENDED]),
              example: customTypeData.example || getExampleData(customTypeData),
              properties: customTypeData.type === 'object' ? {} : undefined,
              required: dataTempStorage.required,
              type: customTypeData.type,
            };
            // object类型的引用类型支持可拓展编辑
            if (customTypeData.type === 'object') {
              _newTypeData[QUOTE_PREFIX] = [{ $ref: `#/components/schemas/${propertyData}` }];
            } else {
              _newTypeData[QUOTE_PREFIX_NO_EXTENDED] = `#/components/schemas/${propertyData}`;
            }

            const typeQuotePath = _newTypeData[API_FORM_KEY];

            update({
              dataTempStorage: _newTypeData,
              innerParamList: [],
              paramListTempStorage: [],
            });

            formRef.current.setFieldsValue({ ..._newTypeData, type: propertyData });
            onChange(dataTempStorage[API_FORM_KEY], _newTypeData, { typeQuotePath, quoteTypeName: propertyData });
            return;
          }
        }

        updater.dataTempStorage(newFormData);
        onChange(dataTempStorage[API_FORM_KEY], newFormData);
      }
    },
    [curPropertyType, dataTempStorage, onChange, props, update, updater],
  );

  const dataTypeOptions = React.useMemo(() => {
    if (!props?.extraDataTypes) {
      return map(BASE_DATA_TYPE, (item) => (
        <Option key={item} value={item}>
          {item.slice(0, 1).toUpperCase() + item.slice(1)}
        </Option>
      ));
    } else {
      const basicDataTypeOptions = map(BASE_DATA_TYPE, (item) => (
        <Option key={item} value={item}>
          {item.slice(0, 1).toUpperCase() + item.slice(1)}
        </Option>
      ));
      const extraOptions =
        map(keys(props.extraDataTypes), (typeName) => (
          <Option key={typeName} value={typeName}>
            {typeName}
          </Option>
        )) || [];

      return [...basicDataTypeOptions, ...extraOptions];
    }
  }, [props]);

  const updateErrorNum = React.useCallback(
    (num, name) => {
      const _formErrorMap: IFormErrorMap = {};
      _formErrorMap[name] = num;
      if (curPropertyType === 'object') {
        forEach(paramListTempStorage, (param) => {
          const pName = param[API_FORM_KEY];
          if (pName === name) {
            _formErrorMap[pName] = num;
          } else {
            _formErrorMap[pName] = formErrorMap[pName] || 0;
          }
        });
      }

      updater.formErrorMap(_formErrorMap);
      const totalError = reduce(values(_formErrorMap), (total, cur) => total + cur, 0);

      props.updateErrorNum && props.updateErrorNum(totalError, dataTempStorage[API_FORM_KEY]);
      props.onFormErrorNumChange && props.onFormErrorNumChange(totalError, dataTempStorage[API_FORM_KEY]);
    },
    [curPropertyType, dataTempStorage, formErrorMap, paramListTempStorage, props, updater],
  );

  const onChangePropertyName = React.useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const newName = e.target.value;
      const { pattern } = regRules.specialLetter;
      const nameMap = formType === 'DataType' ? AllDataTypes : propertyNameMap;
      let isErrorValue = true;

      const isSameWithBaseType = formType === 'DataType' && newName.toLocaleLowerCase() in BASE_DATA_TYPE;
      if (newName !== '' && !pattern.test(newName) && !nameMap.includes(newName) && !isSameWithBaseType) {
        const temp = produce(dataTempStorageRef.current, (draft) => {
          set(draft, API_FORM_KEY, newName);
        });
        isErrorValue = false;

        updater.dataTempStorage(temp);
        onChange(dataTempStorageRef.current[API_FORM_KEY], temp, {
          typeQuotePath: newName,
          quoteTypeName: curPropertyType,
        });
      }

      // 统计name类型错误数量
      const newErrorMap = { ...errorMap };
      if (isErrorValue && !errorMap.name) {
        newErrorMap.name = true;
        updater.errorMap(newErrorMap);
        const errorNum = reduce(values(newErrorMap), (total, cur) => (cur ? total + 1 : total), 0);

        updateErrorNum(errorNum, dataTempStorage[API_FORM_KEY]);
      } else if (!isErrorValue && errorMap.name) {
        newErrorMap.name = false;
        updater.errorMap(newErrorMap);
        const errorNum = reduce(values(newErrorMap), (total, cur) => (cur ? total + 1 : total), 0);

        updateErrorNum(errorNum, dataTempStorage[API_FORM_KEY]);
      }
    },
    [
      AllDataTypes,
      curPropertyType,
      dataTempStorage,
      errorMap,
      formType,
      onChange,
      propertyNameMap,
      updateErrorNum,
      updater,
    ],
  );

  const onChangeNumberValue = React.useCallback(
    ({ propertyKey, propertyData }: { propertyKey: string; propertyData: number }) => {
      let isErrorValue = true;

      if (propertyKey === 'minLength') {
        const maxLength = dataTempStorageRef.current?.maxLength;
        if (maxLength === undefined || maxLength >= propertyData) {
          setFields({ propertyKey, propertyData });
          isErrorValue = false;
        }
      } else if (propertyKey === 'maxLength') {
        const minLength = dataTempStorageRef.current?.minLength;
        if (minLength === undefined || minLength <= propertyData) {
          setFields({ propertyKey, propertyData });
          isErrorValue = false;
        }
      } else if (propertyKey === 'minimum') {
        const maximum = dataTempStorageRef.current?.maximum;
        if (maximum === undefined || maximum >= propertyData) {
          setFields({ propertyKey, propertyData });
          isErrorValue = false;
        }
      } else {
        const minimum = dataTempStorageRef.current?.minimum;
        if (minimum === undefined || minimum <= propertyData) {
          setFields({ propertyKey, propertyData });
          isErrorValue = false;
        }
      }

      // 统计number类型错误数量
      const newErrorMap = { ...errorMap };
      if (isErrorValue && !errorMap[propertyKey]) {
        newErrorMap[propertyKey] = true;
        updater.errorMap(newErrorMap);
        const errorNum = reduce(values(newErrorMap), (total, cur) => (cur ? total + 1 : total), 0);

        updateErrorNum(errorNum, dataTempStorage[API_FORM_KEY]);
      } else if (!isErrorValue && errorMap[propertyKey]) {
        newErrorMap[propertyKey] = false;
        updater.errorMap(newErrorMap);
        const errorNum = reduce(values(newErrorMap), (total, cur) => (cur ? total + 1 : total), 0);

        updateErrorNum(errorNum, dataTempStorage[API_FORM_KEY]);
      }
    },
    [errorMap, dataTempStorage, setFields, updater, updateErrorNum],
  );

  //  参数详情选项
  const propertyFields = React.useMemo(() => {
    const fields = formType !== 'Parameters' ? [descriptionField] : [];
    if (!getRefTypePath(dataTempStorage)) {
      const tempFields = getPropertyDetailFields({ type: curPropertyType, curPropertyType, formData: dataTempStorage });
      fields.push(...tempFields);
    } else if (get(props, `extraDataTypes.${curPropertyType}.type`)) {
      const tempFields = getPropertyDetailFields({
        type: curPropertyType,
        curPropertyType: get(props, ['extraDataTypes', curPropertyType, 'type']),
        formData: dataTempStorage,
      });
      fields.push(...tempFields);
    }

    return map(fields, (fieldItem) => {
      const tempFieldItem = produce(fieldItem, (draft) => {
        if (['minLength', 'maxLength', 'minimum', 'maximum'].includes(draft.name)) {
          set(draft, 'customProps.onChange', (e: React.ChangeEvent<HTMLInputElement> | any) => {
            const newNum = !(Object.prototype.toString.call(e) === '[object Object]') ? e : +e.target.value;
            onChangeNumberValue({ propertyKey: fieldItem?.name, propertyData: newNum });
          });
        } else {
          set(draft, 'customProps.onChange', (e: React.ChangeEvent<HTMLInputElement> | any) => {
            const newVal = !(Object.prototype.toString.call(e) === '[object Object]') ? e : e.target.value;
            setFields({ propertyKey: fieldItem?.name, propertyData: newVal });
          });
        }
        set(draft, 'customProps.disabled', !isEditMode);
      });
      return tempFieldItem;
    });
  }, [formType, getRefTypePath, dataTempStorage, props, curPropertyType, isEditMode, onChangeNumberValue, setFields]);

  const onArrayItemChange = (_formKey: string, _formData: any, extraProps?: Obj) => {
    const newExample = [getExampleData(_formData)];
    const tempData = produce(dataTempStorageRef.current, (draft) => {
      draft.items = _formData;
      draft.example = newExample;
    });
    const _extraProps = {
      quoteTypeName: extraProps?.quoteTypeName,
      typeQuotePath: extraProps?.typeQuotePath
        ? `${dataTempStorageRef.current[API_FORM_KEY]}.${extraProps.typeQuotePath}`
        : '',
    };
    props.onChange(dataTempStorageRef.current[API_FORM_KEY], tempData, _extraProps);
    updater.arrayItemDataStorage(tempData);
  };

  const updateInnerParamList = (formKey: string, _formData: any, extraProps?: Obj) => {
    const tempList = produce(paramListTempStorageRef.current, (draft) => {
      forEach(draft, (item, index) => {
        if (item[API_FORM_KEY] === formKey) {
          draft[index] = _formData;
        }
      });
    });
    const requiredNames: string[] = [];

    const refTypePath = getRefTypePath(dataTempStorage);
    const customDataType = refTypePath ? refTypePath.split('/').slice(-1)[0] : '';

    const objectExample: Obj = { ...getExampleData(get(props, ['extraDataTypes', customDataType])) };

    forEach(tempList, (item) => {
      const _example = item?.example || DATATYPE_EXAMPLE_MAP[item?.type];
      if (item[API_PROPERTY_REQUIRED]) {
        requiredNames.push(item[API_FORM_KEY]);
      }
      objectExample[item[API_FORM_KEY]] = _example;
    });

    updater.paramListTempStorage(tempList);

    if (props.onChange && tempList?.length) {
      const newProperties = {};
      forEach(tempList, (item) => {
        newProperties[item[API_FORM_KEY]] = item;
      });
      const tempData = produce(dataTempStorageRef.current, (draft) => {
        draft.properties = newProperties;
        draft.type = 'object';
        draft.required = requiredNames;
        draft.example = objectExample;
      });
      updater.dataTempStorage(tempData);
      const typeQuotePath = extraProps?.typeQuotePath
        ? `${tempData[API_FORM_KEY] || 'schema'}.properties.${extraProps?.typeQuotePath}`
        : '';
      props.onChange(dataTempStorageRef.current[API_FORM_KEY], tempData, {
        typeQuotePath,
        quoteTypeName: extraProps?.quoteTypeName,
      });
    }
  };

  const deleteParamByFormKey = (_data: Obj, index: number) => {
    const tempList = paramListTempStorage.filter((_record, i) => index !== i);

    updater.innerParamList(tempList);
    updater.paramListTempStorage(tempList);

    const tempProperties = {};
    const newExample = {};
    const requiredNames: string[] = [];

    forEach(tempList, (item) => {
      tempProperties[item[API_FORM_KEY]] = item;
      newExample[item[API_FORM_KEY]] = item?.example;
      item[API_PROPERTY_REQUIRED] && requiredNames.push(item[API_FORM_KEY]);
    });

    const newFormData = produce(dataTempStorage, (draft) => {
      set(draft, 'properties', tempProperties);
      set(draft, 'example', newExample);
      set(draft, 'required', requiredNames);
    });
    updater.dataTempStorage(newFormData);
    props?.onChange && props.onChange(newFormData[API_FORM_KEY], newFormData);
  };

  const getExistNames = React.useCallback(() => {
    const existNames = map(paramListTempStorage, (item) => item[API_FORM_KEY]);

    const refTypePath = getRefTypePath(dataTempStorage);
    const customDataType = refTypePath ? refTypePath.split('/').slice(-1)[0] : '';

    if (refTypePath && get(props, `extraDataTypes.${curPropertyType}.type`) === 'object') {
      const _extraProperties = get(props, ['extraDataTypes', customDataType, 'properties']);
      existNames.push(...keys(_extraProperties));
    }
    return existNames;
  }, [curPropertyType, dataTempStorage, getRefTypePath, paramListTempStorage, props]);

  // object类型的批量添加参数
  const addParamList = React.useCallback(
    (newList: Obj[]) => {
      const refTypePath = getRefTypePath(dataTempStorage);
      const customDataType = refTypePath ? refTypePath.split('/').slice(-1)[0] : '';

      const tempList = [...paramListTempStorage, ...newList];
      updater.innerParamList(tempList);
      updater.paramListTempStorage(tempList);

      const tempProperties = {};
      forEach(tempList, (item) => {
        tempProperties[item[API_FORM_KEY]] = item;
      });
      const newExample = refTypePath ? { ...get(props, `extraDataTypes.${customDataType}.example`) } : {};

      const requiredNames: string[] = [];
      forEach(tempList, (property) => {
        property[API_PROPERTY_REQUIRED] && requiredNames.push(property[API_FORM_KEY]);
        newExample[property[API_FORM_KEY]] = property?.example;
      });

      const newFormData = produce(dataTempStorage, (draft) => {
        set(draft, 'properties', tempProperties);
        set(draft, 'type', 'object');
        set(draft, 'example', newExample);
        set(draft, 'required', requiredNames);
      });
      updater.dataTempStorage(newFormData);
      props?.onChange && props.onChange(dataTempStorage[API_FORM_KEY], newFormData);
    },
    [dataTempStorage, getRefTypePath, paramListTempStorage, props, updater],
  );

  // object添加单个参数
  const addParam = React.useCallback(() => {
    let newPropertyName = `propertyName${innerParamList?.length + 1}`;
    const existNames = getExistNames();

    while (existNames.includes(newPropertyName)) {
      newPropertyName += '1';
    }

    const tempObj = {
      type: 'string',
      example: 'Example',
    };
    tempObj[API_PROPERTY_REQUIRED] = true;
    tempObj[API_FORM_KEY] = newPropertyName;

    addParamList([tempObj]);
  }, [addParamList, getExistNames, innerParamList]);

  // 更新设置example示例
  React.useEffect(() => {
    const tempData = isCurTypeOf(BASE_DATA_TYPE.array) && arrayItemDataStorage ? arrayItemDataStorage : dataTempStorage;
    if (tempData.example && typeof tempData.example === 'object') {
      const newExample = getExampleData(tempData);
      formRef.current.resetFields(['example']);
      formRef.current.setFieldsValue({ example: newExample });
    }
  }, [arrayItemDataStorage, curPropertyType, dataTempStorage, getExampleData, isCurTypeOf]);

  const onCloseParamsModal = () => updater.paramsModalVisible(false);

  const onImport = (importedParams: Obj[]) => {
    onCloseParamsModal();
    addParamList(importedParams);
  };

  const formFieldsSelector = React.useMemo(() => {
    const tempFields = getPropertyFormSelector({
      formType,
      dataTypeOptions,
      propertyNameMap,
      AllDataTypes,
      detailVisible,
      index,
    });
    return map(tempFields, (fieldItem: any) => {
      const tempFieldItem = produce(fieldItem, (draft: { name: string }) => {
        if (draft.name === API_FORM_KEY && ['DataType', 'Query'].includes(formType)) {
          set(draft, 'customProps.onChange', onChangePropertyName);
        } else if (draft.name === 'operation') {
          set(draft, 'customProps.onChange', onToggleDetail);
        } else {
          set(draft, 'customProps.onChange', (e: React.ChangeEvent<HTMLInputElement> | string | boolean) => {
            const newVal = typeof e === 'string' || typeof e === 'boolean' ? e : e.target.value;
            setFields({ propertyKey: fieldItem?.name, propertyData: newVal });
          });
        }
        set(draft, 'customProps.disabled', !isEditMode);
      });
      return tempFieldItem;
    });
  }, [
    formType,
    dataTypeOptions,
    propertyNameMap,
    AllDataTypes,
    detailVisible,
    isEditMode,
    onChangePropertyName,
    onToggleDetail,
    setFields,
    index,
  ]);

  const detailType = React.useMemo(() => {
    if (detailVisible) {
      if (isCurTypeOf(BASE_DATA_TYPE.object)) {
        return 'object';
      } else if (isCurTypeOf(BASE_DATA_TYPE.array)) {
        return 'array';
      } else if (!isBasicDataType) {
        return 'example';
      }
    }
    return '';
  }, [detailVisible, isBasicDataType, isCurTypeOf]);

  return (
    <FormBuilder isMultiColumn ref={formRef}>
      {props?.formType !== 'Parameters' && <Fields fields={formFieldsSelector} />}
      {detailVisible && isBasicDataType && <Fields fields={propertyFields} />}
      {detailType === 'object' && (
        <div>
          {map(innerParamList, (record, index) => {
            return (
              <div className="param-form" key={record[API_FORM_KEY]}>
                {isEditMode && (
                  <div className="param-form-operation">
                    <Popconfirm
                      title={`${i18n.t('common:confirm deletion')}?`}
                      onConfirm={() => deleteParamByFormKey(record, index)}
                    >
                      <CustomIcon type="shanchu" className="param-form-operation-btn cursor-pointer" />
                    </Popconfirm>
                  </div>
                )}
                <div className="param-form-content">
                  <FormBuilder isMultiColumn>
                    <PropertyItemForm
                      key={record[API_FORM_KEY]}
                      updateErrorNum={updateErrorNum}
                      formData={record}
                      isEditMode={isEditMode}
                      onChange={updateInnerParamList}
                      extraDataTypes={props?.extraDataTypes}
                      allExtraDataTypes={props?.allExtraDataTypes}
                      siblingProperties={filter(
                        paramListTempStorage,
                        (item) => item[API_FORM_KEY] !== record[API_FORM_KEY],
                      )}
                      index={index}
                    />
                  </FormBuilder>
                </div>
              </div>
            );
          })}
          {isEditMode && (
            <>
              <Button className="operation-btn mb-4" onClick={addParam}>
                {i18n.t('common:add parameter')}
              </Button>
              <Button className="operation-btn mb-4 ml-2" onClick={() => updater.paramsModalVisible(true)}>
                {i18n.t('dop:import parameters')}
              </Button>
            </>
          )}
          {props?.formType !== 'Parameters' && <Fields fields={[objectExampleField]} />}
        </div>
      )}
      {detailType === 'array' && (
        <>
          {isBasicDataType && (
            <div className="array-form">
              <PropertyItemForm
                formType="Array"
                updateErrorNum={updateErrorNum}
                formData={dataTempStorage.items || {}}
                detailVisible
                onChange={onArrayItemChange}
                isEditMode={isEditMode}
                extraDataTypes={props?.extraDataTypes}
                allExtraDataTypes={props?.allExtraDataTypes}
                siblingProperties={filter(
                  paramListTempStorage,
                  (item) => item[API_FORM_KEY] !== dataTempStorage.items[API_FORM_KEY],
                )}
              />
            </div>
          )}
          <Fields fields={[objectExampleField]} />
        </>
      )}
      {detailType === 'example' && <Fields fields={[objectExampleField]} />}
      <ApiParamsModal
        visible={paramsModalVisible}
        onImport={onImport}
        onClose={onCloseParamsModal}
        paramList={paramListTempStorage}
      />
    </FormBuilder>
  );
});

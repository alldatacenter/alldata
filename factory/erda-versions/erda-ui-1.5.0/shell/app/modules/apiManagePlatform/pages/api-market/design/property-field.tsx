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
import { Checkbox, Radio, Tooltip, Input, Select, InputNumber } from 'antd';
import i18n from 'i18n';
import { FileEditor, Icon as CustomIcon } from 'common';
import { useUpdate } from 'common/use-hooks';
import { map, isEmpty } from 'lodash';
import { CustomLabel } from 'dcos/common/custom-label';

import { regRules } from 'common/utils';
import {
  BASE_DATA_TYPE,
  NUMBER_TYPE_MAP,
  INTEGER_TYPE_MAP,
  REQUIRED_OPTIONS,
  API_PROPERTY_REQUIRED,
  API_MEDIA,
  RADIO_OPTIONS,
  DATATYPE_EXAMPLE_MAP,
  API_FORM_KEY,
  INPUT_MAX_LENGTH,
  TEXTAREA_MAX_LENGTH,
  DEFAULT_NUMBER_PROPS,
  API_MEDIA_TYPE,
  DEFAULT_LENGTH_PROPS,
} from 'app/modules/apiManagePlatform/configs.ts';

const { Option } = Select;
const numberTypeOptions = map(NUMBER_TYPE_MAP, (item) => (
  <Option key={item} value={item}>
    {`${item.slice(0, 1).toUpperCase()}${item.slice(1)}`}
  </Option>
));

const integerTypeOptions = map(INTEGER_TYPE_MAP, (item) => (
  <Option key={item} value={item}>
    {`${item.slice(0, 1).toUpperCase()}${item.slice(1)}`}
  </Option>
));

export const RadioGroup = React.forwardRef(
  (radioProps: {
    options: Array<{ name: string; value: string }>;
    value: string;
    disabled?: boolean;
    onChange: (e: any) => void;
  }) => {
    const { options, onChange, value, disabled } = radioProps;

    return (
      <Radio.Group buttonStyle="solid" size="small" value={value} onChange={onChange} disabled={disabled}>
        {options.map(({ value: v, name }) => (
          <Radio.Button key={v} value={v}>
            {name}
          </Radio.Button>
        ))}
      </Radio.Group>
    );
  },
);

const DetailBtn = (detailBtnProps: { visible: boolean; onChange: (v: boolean) => void }) => {
  const { visible, onChange } = detailBtnProps;
  return (
    <Tooltip title={i18n.t('detail')}>
      <CustomIcon
        type={visible ? 'chevron-up' : 'chevron-down'}
        className="cursor-pointer mt-2"
        style={{ width: 'auto' }}
        onClick={() => {
          onChange(!visible);
        }}
      />
    </Tooltip>
  );
};

// Enumerated values
export const EnumRef = React.forwardRef(
  (enumProps: {
    value: string[];
    dataType: BASE_DATA_TYPE;
    hideCheckBox?: boolean;
    disabled?: boolean;
    onChange: (v?: string[] | null) => void;
  }) => {
    const [{ enumVisible }, updater] = useUpdate({
      enumVisible: false,
    });

    const { onChange, disabled = false, dataType, ...restProps } = enumProps;

    React.useEffect(() => {
      if (enumProps?.value && enumProps?.value.length) {
        updater.enumVisible(true);
      }
    }, [enumProps, updater]);

    const onCheckHandle = React.useCallback(
      (e) => {
        const checkState = e.target.checked;
        updater.enumVisible(checkState);
        if (!checkState) {
          onChange(null);
        }
      },
      [onChange, updater],
    );

    const onChangeHandle = (values: any[]) => {
      const enumValues = !isEmpty(values)
        ? map(values, (item) => {
            return dataType === BASE_DATA_TYPE.number ? Number(item) || 0 : item;
          })
        : undefined;
      onChange(enumValues);
    };

    const isEnumExist = React.useMemo(() => enumProps?.value && enumProps?.value?.length, [enumProps]);
    const showAddBtn = React.useMemo(() => enumVisible || isEnumExist, [enumVisible, isEnumExist]);

    return (
      <div className="flex items-center justify-start">
        {disabled ? (
          <div>
            <span className="mr-2">{i18n.t('dop:enumerated value')}: </span>
            {isEnumExist && map(enumProps?.value, (item) => <span className="tag-default">{item}</span>)}
          </div>
        ) : (
          <>
            {!restProps?.hideCheckBox && (
              <Checkbox disabled={disabled} checked={enumVisible} onChange={onCheckHandle}>
                {i18n.t('dop:enumerated value')}{' '}
              </Checkbox>
            )}
            {showAddBtn && (
              <div style={{ whiteSpace: 'pre-wrap' }}>
                <CustomLabel
                  value={enumProps?.value || []}
                  onChange={onChangeHandle}
                  labelName={i18n.t('dop:add enum value')}
                />
              </div>
            )}
          </>
        )}
      </div>
    );
  },
);

// example of object type
export const ApiFileEditor = React.forwardRef((fileEditorProps: { value: any }) => {
  const { value } = fileEditorProps;
  const _value = value ? JSON.stringify(value, null, 2) : JSON.stringify({}, null, 2);
  return <FileEditor {...fileEditorProps} fileExtension="json" value={_value} />;
});

/** ------Configuration items for boolean-------------------------------*/
// boolean default value
export const booleanDefaultValueField = {
  type: RadioGroup,
  label: i18n.t('default value'),
  name: 'default',
  colSpan: 6,
  required: false,
  customProps: {
    options: RADIO_OPTIONS,
  },
};

// boolean example
export const booleanExampleField = {
  type: RadioGroup,
  label: i18n.t('dop:example'),
  name: 'example',
  colSpan: 6,
  required: false,
  initialValue: true,
  customProps: {
    options: RADIO_OPTIONS,
  },
};

/** ------Configuration items for string-------------------------------*/
// string rules
export const stringPatternField = {
  type: Input,
  label: i18n.t('dop:validation rules'),
  name: 'pattern',
  required: false,
  colSpan: 8,
  customProps: {
    maxLength: INPUT_MAX_LENGTH,
  },
};

// string min
export const stringMinLengthField = (dataTempStorage: Obj) => {
  return {
    type: InputNumber,
    label: i18n.t('dop:minimum length'),
    name: 'minLength',
    colSpan: 8,
    required: false,
    customProps: {
      className: 'w-full',
      precision: 0,
      ...DEFAULT_LENGTH_PROPS,
    },
    rules: [
      {
        validator: (_rule: any, value: number, callback: (msg?: string) => void) => {
          const maxLength = dataTempStorage?.maxLength;
          if (maxLength === undefined || maxLength >= value) {
            callback();
          } else {
            callback(i18n.t('dop:the minimum value must be less than or equal to the maximum value'));
          }
        },
      },
    ],
  };
};
// string max
export const stringMaxLengthField = (dataTempStorage: Obj) => {
  return {
    type: InputNumber,
    label: i18n.t('dop:the maximum length'),
    name: 'maxLength',
    colSpan: 8,
    required: false,
    customProps: {
      className: 'w-full',
      precision: 0,
      ...DEFAULT_LENGTH_PROPS,
    },
    rules: [
      {
        validator: (_rule: any, value: number, callback: (msg?: string) => void) => {
          const minLength = dataTempStorage?.minLength;
          if (minLength === undefined || minLength <= value) {
            callback();
          } else {
            callback(i18n.t('dop:the maximum value must be greater than or equal to the minimum value'));
          }
        },
      },
    ],
  };
};

// string default value
export const stringDefaultValueField = {
  type: Input,
  label: i18n.t('default value'),
  name: 'default',
  required: false,
  colSpan: 24,
  customProps: {
    maxLength: INPUT_MAX_LENGTH,
    className: 'w-full',
  },
};

// string example
export const stringExampleField = {
  type: Input,
  label: i18n.t('dop:example'),
  name: 'example',
  colSpan: 24,
  required: false,
  initialValue: DATATYPE_EXAMPLE_MAP.string,
  customProps: {
    maxLength: TEXTAREA_MAX_LENGTH,
    className: 'w-full',
  },
};

/** ------Configuration items for number-------------------------------*/
// number value type
export const numberFormatField = {
  type: Select,
  label: i18n.t('dop:numeric type'),
  name: 'format',
  required: false,
  colSpan: 8,
  customProps: {
    children: numberTypeOptions,
  },
};

// number min
export const numberMinimumField = (dataTempStorage: Obj) => {
  return {
    type: InputNumber,
    label: i18n.t('msp:minimum'),
    name: 'minimum',
    colSpan: 8,
    required: false,
    customProps: {
      className: 'w-full',
      ...DEFAULT_NUMBER_PROPS,
    },
    rules: [
      {
        validator: (_rule: any, value: number, callback: (msg?: string) => void) => {
          const maximum = dataTempStorage?.maximum;
          if (maximum === undefined || maximum >= value) {
            callback();
          } else {
            callback(i18n.t('dop:the minimum value must be less than or equal to the maximum value'));
          }
        },
      },
    ],
  };
};

// number max
export const numberMaximumField = (dataTempStorage: Obj) => {
  return {
    type: InputNumber,
    label: i18n.t('msp:maximum value'),
    name: 'maximum',
    colSpan: 8,
    required: false,
    customProps: {
      className: 'w-full',
      ...DEFAULT_NUMBER_PROPS,
    },
    rules: [
      {
        validator: (_rule: any, value: number, callback: (msg?: string) => void) => {
          const minimum = dataTempStorage?.minimum;
          if (minimum === undefined || minimum <= value) {
            callback();
          } else {
            callback(i18n.t('dop:the maximum value must be greater than or equal to the minimum value'));
          }
        },
      },
    ],
  };
};

// number defaule value
export const numberDefaultValueField = {
  type: InputNumber,
  label: i18n.t('default value'),
  name: 'default',
  required: false,
  colSpan: 24,
  customProps: { className: 'w-full', ...DEFAULT_NUMBER_PROPS },
};

// number example
export const numberExampleField = {
  type: InputNumber,
  label: i18n.t('dop:example'),
  name: 'example',
  colSpan: 24,
  required: false,
  initialValue: DATATYPE_EXAMPLE_MAP.number,
  customProps: { className: 'w-full', ...DEFAULT_NUMBER_PROPS },
};

/** ------Configuration items for integer-------------------------------*/
// value type of integer
export const integerFormatField = {
  type: Select,
  label: i18n.t('dop:numeric type'),
  name: 'format',
  required: false,
  colSpan: 8,
  customProps: {
    children: integerTypeOptions,
  },
};

/** ------Configuration items for object-------------------------------*/
// object example configuration items
export const objectExampleField = {
  type: ApiFileEditor,
  label: i18n.t('dop:example'),
  name: 'example',
  colSpan: 24,
  required: false,
  customProps: {
    className: 'w-full',
    fileExtension: 'json',
    readOnly: true,
  },
};

// description field
export const descriptionField = {
  type: Input.TextArea,
  label: i18n.t('description'),
  name: 'description',
  colSpan: 24,
  required: false,
  customProps: { rows: 2, maxLength: TEXTAREA_MAX_LENGTH },
};

// configuration of enumeration values
export const enumField = (curPropertyType: string) => {
  return {
    type: EnumRef,
    label: '',
    name: 'enum',
    colSpan: 24,
    required: false,
    customProps: {
      dataType: curPropertyType,
    },
  };
};

// media type field
export const mediaTypeField = {
  type: Select,
  label: 'Media Type',
  name: API_MEDIA,
  colSpan: 12,
  initialValue: 'application/json',
  customProps: {
    children: map(API_MEDIA_TYPE, (t) => (
      <Option key={t} value={t}>
        {t}
      </Option>
    )),
  },
};

export const propertyTypeSelectorField = {
  type: Select,
  label: i18n.t('type'),
  name: 'type',
  colSpan: 12,
  initialValue: 'object',
};

export const detailBtnField = {
  type: DetailBtn,
  label: <span style={{ opacity: 0 }}>s</span>,
  name: 'operation',
  colSpan: 1.5,
  initialValue: 'false',
  required: false,
};

export const getPropertyDetailFields = (props: {
  type: BASE_DATA_TYPE;
  curPropertyType: BASE_DATA_TYPE | string;
  formData: Obj;
}): any[] => {
  const { type, curPropertyType, formData } = props;
  if (type === BASE_DATA_TYPE.string || formData?.type === BASE_DATA_TYPE.string) {
    return [
      { ...enumField(curPropertyType) },
      stringPatternField,
      { ...stringMinLengthField(formData) },
      { ...stringMaxLengthField(formData) },
      stringDefaultValueField,
      stringExampleField,
    ];
  } else if (type === BASE_DATA_TYPE.number || formData?.type === BASE_DATA_TYPE.number) {
    return [
      { ...enumField(curPropertyType) },
      numberFormatField,
      { ...numberMinimumField(formData) },
      { ...numberMaximumField(formData) },
      numberDefaultValueField,
      numberExampleField,
    ];
  } else if (type === BASE_DATA_TYPE.integer) {
    return [
      { ...enumField(curPropertyType) },
      integerFormatField,
      { ...numberMinimumField(formData) },
      { ...numberMaximumField(formData) },
      numberDefaultValueField,
      numberExampleField,
    ];
  } else if (type === BASE_DATA_TYPE.boolean) {
    return [booleanDefaultValueField, booleanExampleField];
  } else {
    return [];
  }
};

type IFormType = 'Response' | 'Query' | 'Parameters' | 'DataType' | 'Body' | 'Array';
export const getPropertyFormSelector = (props: {
  formType: IFormType;
  dataTypeOptions: any[];
  propertyNameMap: string[];
  AllDataTypes: string[];
  detailVisible: boolean;
  index?: number;
}) => {
  const { formType, dataTypeOptions, propertyNameMap, AllDataTypes, detailVisible, index } = props;
  if (formType === 'Response' || formType === 'Body') {
    return [
      mediaTypeField,
      {
        ...propertyTypeSelectorField,
        customProps: {
          children: dataTypeOptions,
          showSearch: true,
        },
      },
    ];
  } else if (formType === 'Query') {
    return [
      {
        type: Input,
        label: i18n.t('backup:parameter name'),
        name: API_FORM_KEY,
        colSpan: 10,
        customProps: {
          maxLength: INPUT_MAX_LENGTH,
        },
        rules: [
          {
            validator: (_rule: any, value: string, callback: (msg?: string) => void) => {
              const { pattern, message } = regRules.specialLetter;
              const nameMap = [...propertyNameMap];
              if (index || index === 0) {
                nameMap.splice(index, 1);
              }

              if (nameMap.includes(value)) {
                callback(i18n.t('dop:the parameter names cannot be the same'));
              } else if (pattern.test(value)) {
                callback(message);
              } else {
                callback();
              }
            },
          },
        ],
      },
      {
        type: RadioGroup,
        label: i18n.t('is it required'),
        name: API_PROPERTY_REQUIRED,
        colSpan: 2.5,
        initialValue: true,
        customProps: {
          options: REQUIRED_OPTIONS,
        },
      },
      {
        ...propertyTypeSelectorField,
        colSpan: 10,
        initialValue: 'string',
        customProps: {
          children: dataTypeOptions,
          showSearch: true,
        },
      },
      {
        ...detailBtnField,
        customProps: {
          visible: detailVisible,
        },
      },
    ];
  } else if (formType === 'DataType') {
    return [
      {
        type: Input,
        label: i18n.t('backup:parameter name'),
        name: API_FORM_KEY,
        colSpan: 12,
        customProps: {
          maxLength: INPUT_MAX_LENGTH,
        },
        rules: [
          {
            validator: (_rule: any, value: string, callback: (msg?: string) => void) => {
              const { pattern, message } = regRules.specialLetter;
              if (AllDataTypes.includes(value)) {
                callback(i18n.t('the same {key} exists', { key: i18n.t('name') }));
              } else if (pattern.test(value)) {
                callback(message);
              } else if (value.toLocaleLowerCase() in BASE_DATA_TYPE) {
                callback(
                  i18n.t(
                    'dop:cannot enter uppercase or lowercase letters same as the basic data type, including string, String, STRING, sTring, etc.',
                  ),
                );
              } else {
                callback();
              }
            },
          },
        ],
      },
      {
        ...propertyTypeSelectorField,
        initialValue: 'string',
        customProps: {
          children: dataTypeOptions,
          showSearch: true,
        },
      },
    ];
  } else if (formType === 'Array') {
    return [
      {
        ...propertyTypeSelectorField,
        initialValue: 'string',
        colSpan: 24,
        customProps: {
          children: dataTypeOptions,
          showSearch: true,
        },
      },
    ];
  } else {
    return {};
  }
};

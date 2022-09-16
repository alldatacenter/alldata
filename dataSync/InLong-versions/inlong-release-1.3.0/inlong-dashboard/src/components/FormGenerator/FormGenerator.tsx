/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import React, { useState, useEffect, useCallback } from 'react';
import merge from 'lodash/merge';
import { trim } from '@/utils';
import { Form } from 'antd';
import type { FormProps } from 'antd';
import FormItemContent, { FormItemProps as ItemType } from './FormItemContent';

export interface FormItemProps extends Omit<ItemType, 'props'> {
  props?: ItemType['props'] | ((values: Record<string, any>) => ItemType['props']);
}

// Generator properties
export interface FormGeneratorProps extends FormProps {
  // content
  content?: FormItemProps[];
  // contents
  contents?: ContentsItemProps[];
  // Whether to use the default maximum width
  useMaxWidth?: boolean | number;
  style?: React.CSSProperties;
  // The current form value, under normal circumstances there is no need to pass in
  // When externally using methods such as setFieldsValue to change the value, the internal state refresh can be triggered by changing the prop
  allValues?: Record<string, unknown>;
  // onFilter is similar to onValuesChange, with custom trigger conditions added, for example, when the search box is entered
  // At the same time, the return value executes trim, if you need noTrim, you need to pay attention (such as password)
  // Currently holding input, inputsearch
  onFilter?: Function;
}

export interface ContentsItemProps {
  // Render target, if not, the current component
  target?: undefined | null | any;
  content: FormItemProps[];
}

const getContentMap = content => {
  return content.reduce(
    (acc, cur) => ({
      ...acc,
      [cur.name]: cur,
    }),
    {},
  );
};

const FormGenerator: React.FC<FormGeneratorProps> = props => {
  // eslint-disable-next-line
  const [form] = useState(props.form || Form.useForm()[0]);

  // Record real-time values
  const [realTimeValues, setRealTimeValues] = useState<Record<string, unknown>>(
    props.allValues || {},
  );
  const [contents, setContents] = useState<ContentsItemProps[]>([]);

  const combineContentWithProps = useCallback(
    (initialContent: Record<string, any>[], props: FormGeneratorProps) => {
      return initialContent.map((v: any) => {
        const initialProps =
          typeof v.props === 'function' ? v.props(realTimeValues) : v.props || {};
        const namePath = Array.isArray(v.name) ? v.name : v.name && v.name.split('.');
        const name = namePath && namePath.length > 1 ? namePath : v.name;
        // props hold
        const holdProps = {} as any;
        if (v.type === 'inputsearch') {
          // Hold onSearch to trigger onFilter
          holdProps.onSearch = (value, event) => {
            initialProps.onSearch && initialProps.onSearch(value, event);
            props.onFilter &&
              props.onFilter({
                ...realTimeValues,
                [name]: value,
              });
          };
        } else if (v.type === 'input') {
          // Hold onPressEnter to trigger onFilter
          holdProps.onPressEnter = event => {
            initialProps.onPressEnter && initialProps.onPressEnter(event);
            props.onFilter && props.onFilter(realTimeValues);
          };
        }
        if (initialProps.onChange) {
          // Hold onChange, you can return an object, and automatically setFieldsValue(object) each time onChange.
          // Note that the key value must be collected (cooperate with extraNames), otherwise the set will be invalid
          holdProps.onChange = (...rest) => {
            const mappingValues = initialProps.onChange(...rest);
            if (mappingValues) {
              form.setFieldsValue(mappingValues);
              setRealTimeValues(prev => ({ ...merge(prev, mappingValues) }));
              props.onValuesChange &&
                props.onValuesChange(mappingValues, merge(mappingValues, form.getFieldsValue()));
            }
          };
        }

        if (v.suffix?.name) {
          const suffixNp = Array.isArray(v.suffix.name) ? v.suffix.name : v.suffix.name.split('.');
          v.suffix.name = suffixNp && suffixNp.length > 1 ? suffixNp : v.suffix.name;
        }

        return {
          ...v,
          name,
          props: {
            ...initialProps,
            ...holdProps,
          },
        };
      });
    },
    [realTimeValues, form],
  );

  // A real-time value is generated when it is first mounted, because the initialValue may be defined on the FormItem
  useEffect(() => {
    if (props.allValues) {
      setRealTimeValues(props.allValues);
    } else if (form) {
      const timmer = setTimeout(() => {
        const { getFieldsValue } = form;
        const values = getFieldsValue(true);
        setRealTimeValues(prev => ({ ...prev, ...values }));
      }, 0);
      return () => clearTimeout(timmer);
    }
  }, [form, props.allValues]);

  useEffect(() => {
    if (!props.contents) {
      setContents([
        {
          content: props.content ? combineContentWithProps(props.content, props) : [],
        },
      ]);
    } else {
      setContents(
        props.contents.map(v => {
          return {
            ...v,
            content: combineContentWithProps(v.content, props),
          };
        }),
      );
    }
  }, [props.content, props.contents, props, combineContentWithProps]);

  const { layout = 'horizontal', useMaxWidth } = props;
  const isHorizontal = layout === 'horizontal';
  const {
    labelCol = isHorizontal && !props.labelCol ? { span: 6 } : props.labelCol,
    wrapperCol = isHorizontal && !props.labelCol ? { span: 18 } : props.wrapperCol,
    labelAlign = 'left',
    style = useMaxWidth
      ? {
          ...props.style,
          maxWidth: typeof useMaxWidth === 'number' ? useMaxWidth : 1200,
        }
      : props.style,
    children,
    onFilter,
  } = props;

  const isInline = layout === 'inline';

  const onValuesChange = (changedValues, allValues) => {
    props.onValuesChange && props.onValuesChange(changedValues, allValues);

    if (contents && contents.length) {
      const itemMap = getContentMap(contents[0].content);
      const noPrevent = Object.keys(changedValues).some(key => {
        const type = itemMap[key] && itemMap[key].type;
        return type !== 'input' && type !== 'inputsearch';
      });
      const newRealTimeValues = trim(allValues) as any;
      setRealTimeValues(newRealTimeValues);
      if (noPrevent && onFilter) {
        onFilter(newRealTimeValues);
      }
    }
  };

  const formProps = { ...props };
  delete formProps.useMaxWidth;
  delete formProps.content;
  delete formProps.contents;
  delete formProps.onFilter;
  delete formProps.allValues;

  return (
    <Form
      {...formProps}
      form={form}
      layout={layout}
      labelCol={labelCol}
      wrapperCol={wrapperCol}
      labelAlign={labelAlign}
      style={style}
      onValuesChange={onValuesChange}
    >
      {contents &&
        contents.map((val: ContentsItemProps, index) => (
          <FormItemContent
            content={val.content}
            target={val.target}
            useInline={isInline}
            key={index}
            values={realTimeValues}
          />
        ))}
      {children}
    </Form>
  );
};

export const useForm = Form.useForm;

export default FormGenerator;

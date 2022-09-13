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

import React, { useMemo } from 'react';
import { createPortal } from 'react-dom';
import { Col, Row, Form, Space } from 'antd';
import { FormItemProps as AntdFormItemProps } from 'antd/lib/form';
import plugins from './plugins';

type PluginsTypes = keyof typeof plugins;

type SuffixDefineType = Omit<FormItemProps, 'col' | 'suffix'>;

// Single formItem
export interface FormItemProps extends AntdFormItemProps {
  type: PluginsTypes | React.ReactNode;
  visible?: boolean | ((values: Record<string, any>) => boolean);
  props?: Record<string, any>;
  col?: number;
  // As ID
  name?: AntdFormItemProps['name'];
  suffix?: React.ReactNode | SuffixDefineType;
  // The extra name will not be rendered on the page, but will be automatically collected when the form getValues
  extraNames?: string[];
}

export interface FormItemContentProps {
  // Render list
  content: FormItemProps[];
  // Whether to use inline rendering
  useInline?: boolean;
  // Render target
  target?: Element;
  // Real-time value of Form
  values?: Record<string, unknown>;
}

const FormItemContent: React.FC<FormItemContentProps> = ({
  content,
  useInline = false,
  target,
  values = {},
}) => {
  // Form atomic component
  const Comp = useMemo(
    () =>
      ({ type, ...props }: { type: PluginsTypes }) => {
        const Comp = plugins[(type as string) || 'input'];
        return <Comp {...props} />;
      },
    [],
  );

  const FormItem = useMemo(
    () =>
      ({ type: T, formItemProps, useSpace, props }) => {
        return (
          <Form.Item {...formItemProps} noStyle={useSpace}>
            {typeof T === 'string' ? (
              <Comp type={T} {...props} />
            ) : React.isValidElement(T) ? (
              T
            ) : (
              <T {...props} />
            )}
          </Form.Item>
        );
      },
    // eslint-disable-next-line
    [],
  );

  const Content = useMemo(
    () =>
      ({ useSpace, suffix, extra, children, label, required, style }) =>
        useSpace ? (
          <Form.Item label={label} required={required} style={style} extra={extra}>
            <Space>
              {children}
              {suffix}
            </Space>
          </Form.Item>
        ) : (
          children
        ),
    [],
  );

  const body = useMemo(
    () =>
      content.map(
        (
          { visible = true, type = 'text', col, suffix, props, extraNames = [], ...formItemProps },
          index,
        ) => {
          if (visible === false || (typeof visible === 'function' && !!visible(values) === false)) {
            return null;
          }
          const key = formItemProps.name || index.toString();
          const useSpace = !!suffix;
          (formItemProps.wrapperCol as any) = formItemProps.label ? undefined : 24;

          const inner = (
            <Content
              key={key.toString()}
              label={formItemProps.label}
              extra={formItemProps.extra}
              required={formItemProps.rules?.some(item => (item as any).required)}
              suffix={(() => {
                if ((suffix as SuffixDefineType)?.type) {
                  const {
                    type,
                    visible = true,
                    props,
                    ...formItemProps
                  } = suffix as SuffixDefineType;
                  return (
                    ((typeof visible === 'boolean' && visible !== false) ||
                      (typeof visible === 'function' && visible(values) !== false)) && (
                      <FormItem
                        type={type}
                        formItemProps={formItemProps}
                        useSpace={useSpace}
                        props={props}
                      />
                    )
                  );
                }

                return suffix;
              })()}
              useSpace={useSpace}
              style={formItemProps.style}
            >
              <FormItem
                type={type}
                formItemProps={formItemProps}
                useSpace={useSpace}
                props={props}
              />
              {extraNames.map(nameItem => (
                <FormItem
                  key={nameItem}
                  type="text"
                  formItemProps={{ name: nameItem, hidden: true }}
                  useSpace={false}
                  props={{}}
                />
              ))}
            </Content>
          );

          return useInline ? (
            inner
          ) : (
            <Col key={key.toString()} span={col || 24}>
              {inner}
            </Col>
          );
        },
      ),
    // eslint-disable-next-line
    [content, useInline, values],
  );

  const renderBody = useInline ? <>{body}</> : <Row gutter={20}>{body}</Row>;
  const targetResult = target ? createPortal(renderBody, target) : renderBody;
  return targetResult;
};

export default FormItemContent;

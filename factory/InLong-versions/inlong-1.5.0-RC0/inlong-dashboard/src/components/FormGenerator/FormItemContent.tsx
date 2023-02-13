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

import React, { useMemo, useState } from 'react';
import { createPortal } from 'react-dom';
import { Col, Row, Form, Space } from 'antd';
import { FormItemProps as AntdFormItemProps } from 'antd/lib/form';
import TextSwitch from '@/components/TextSwitch';
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
  isPro?: boolean;
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

// Form atomic component
const Comp = ({ type, ...props }: { type: PluginsTypes }) => {
  const Comp = plugins[(type as string) || 'input'];
  return <Comp {...props} />;
};

const FormItem = ({ type: T, formItemProps, useSpace, props }) => {
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
};

const Content = ({ useSpace, suffix, extra, children, label, required, style }) => {
  return useSpace ? (
    <Form.Item label={label} required={required} style={style} extra={extra}>
      <Space>
        {children}
        {suffix}
      </Space>
    </Form.Item>
  ) : (
    children
  );
};

const FormItemContent: React.FC<FormItemContentProps> = ({
  content,
  useInline = false,
  target,
  values = {},
}) => {
  const [proOpened, setProOpened] = useState(false);

  const body = useMemo(() => {
    let proIndex = -1;
    const conts = content.map(
      (
        {
          visible = true,
          type = 'text',
          col,
          suffix,
          props,
          extraNames = [],
          isPro,
          ...formItemProps
        },
        index,
      ) => {
        if (
          visible === false ||
          (typeof visible === 'function' && values && !!visible(values) === false)
        ) {
          return null;
        }
        if (isPro) {
          if (proIndex === -1) proIndex = index;
          formItemProps.hidden = !proOpened || Boolean(formItemProps.hidden);
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
            <FormItem type={type} formItemProps={formItemProps} useSpace={useSpace} props={props} />
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
          <Col
            key={key.toString()}
            span={col || 24}
            style={formItemProps.hidden ? { display: 'none' } : {}}
          >
            {inner}
          </Col>
        );
      },
    );
    if (proIndex >= 0) {
      conts.splice(
        proIndex,
        0,
        <Col key="_pro" span={24}>
          <TextSwitch value={proOpened} onChange={v => setProOpened(v)} />
        </Col>,
      );
    }

    return conts;
  }, [content, useInline, values, proOpened]);

  const renderBody = useInline ? <>{body}</> : <Row gutter={20}>{body}</Row>;
  const targetResult = target ? createPortal(renderBody, target) : renderBody;
  return targetResult;
};

export default FormItemContent;

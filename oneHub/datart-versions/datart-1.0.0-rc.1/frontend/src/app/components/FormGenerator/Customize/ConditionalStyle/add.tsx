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

import { Col, Form, Input, InputNumber, Modal, Radio, Row, Select } from 'antd';
import { ColorPickerPopover } from 'app/components/ColorPicker';
import { DataViewFieldType } from 'app/constants';
import { memo, useEffect, useState } from 'react';
import styled from 'styled-components';
import {
  ConditionalOperatorTypes,
  ConditionalStyleFormValues,
  ConditionalStyleRange,
  OperatorTypes,
  OperatorTypesLocale,
} from './types';

interface AddProps {
  context?: any;
  translate?: (title: string, options?: any) => string;
  visible: boolean;
  values: ConditionalStyleFormValues;
  onOk: (values: ConditionalStyleFormValues) => void;
  onCancel: () => void;
}

export default function Add({
  translate: t = title => title,
  values,
  visible,
  onOk,
  onCancel,
  context: { label, type },
}: AddProps) {
  const [colors] = useState([
    {
      name: 'background',
      label: t('conditionalStyleTable.header.color.background'),
      value: undefined,
    },
    {
      name: 'textColor',
      label: t('conditionalStyleTable.header.color.text'),
      value: undefined,
    },
  ]);
  const [operatorSelect, setOperatorSelect] = useState<
    { label: string; value: string }[]
  >([]);
  const [operatorValue, setOperatorValue] = useState<OperatorTypes>(
    OperatorTypes.Equal,
  );
  const [form] = Form.useForm<ConditionalStyleFormValues>();

  useEffect(() => {
    if (type) {
      setOperatorSelect(
        ConditionalOperatorTypes[type]?.map(item => ({
          label: `${OperatorTypesLocale[item]} [${item}]`,
          value: item,
        })),
      );
    } else {
      setOperatorSelect([]);
    }
  }, [type]);

  useEffect(() => {
    // !重置form
    if (visible) {
      const result: Partial<ConditionalStyleFormValues> =
        Object.keys(values).length === 0
          ? {
              range: ConditionalStyleRange.Cell,
              operator: OperatorTypes.Equal,
            }
          : values;

      form.setFieldsValue(result);
      setOperatorValue(result.operator ?? OperatorTypes.Equal);
    }
  }, [form, visible, values, label]);

  const modalOk = () => {
    form.validateFields().then(values => {
      onOk({
        ...values,
        target: {
          name: label,
          type,
        },
      });
    });
  };

  const operatorChange = (value: OperatorTypes) => {
    setOperatorValue(value);
  };

  const renderValueNode = () => {
    let DefaultNode = <></>;
    switch (type) {
      case DataViewFieldType.NUMERIC:
        DefaultNode = <InputNumber />;
        break;
      default:
        DefaultNode = <Input />;
        break;
    }

    switch (operatorValue) {
      case OperatorTypes.In:
      case OperatorTypes.NotIn:
        return (
          <Select
            mode="tags"
            notFoundContent={
              <>{t('conditionalStyleTable.modal.notFoundContent')}</>
            }
          />
        );
      case OperatorTypes.Between:
        return <InputNumberScope />;
      default:
        return DefaultNode;
    }
  };

  return (
    <Modal
      destroyOnClose
      title={t('conditionalStyleTable.modal.title')}
      visible={visible}
      onOk={modalOk}
      onCancel={onCancel}
    >
      <Form
        name="conditional-style-form"
        labelAlign="left"
        labelCol={{ span: 8 }}
        wrapperCol={{ span: 16 }}
        preserve={false}
        form={form}
        autoComplete="off"
      >
        <Form.Item label="uid" name="uid" hidden>
          <Input />
        </Form.Item>

        <Form.Item
          label={t('conditionalStyleTable.header.range.title')}
          name="range"
          rules={[{ required: true }]}
        >
          <Radio.Group>
            <Radio.Button value="cell">
              {t('conditionalStyleTable.header.range.cell')}
            </Radio.Button>
            <Radio.Button value="row">
              {t('conditionalStyleTable.header.range.row')}
            </Radio.Button>
          </Radio.Group>
        </Form.Item>

        <Form.Item
          label={t('conditionalStyleTable.header.operator')}
          name="operator"
          rules={[{ required: true }]}
        >
          <Select options={operatorSelect} onChange={operatorChange} />
        </Form.Item>

        {operatorValue !== OperatorTypes.IsNull ? (
          <Form.Item
            label={t('conditionalStyleTable.header.value')}
            name="value"
            rules={[{ required: true }]}
          >
            {renderValueNode()}
          </Form.Item>
        ) : null}

        <Form.Item label={t('conditionalStyleTable.header.color.title')}>
          <Row gutter={24} align="middle">
            {colors.map(({ label, value, name }) => (
              <Form.Item key={label} name={['color', name]} noStyle>
                <ColorSelector label={label} value={value} />
              </Form.Item>
            ))}
          </Row>
        </Form.Item>
      </Form>
    </Modal>
  );
}

const ColorSelector = memo(
  ({
    label,
    value,
    onChange,
  }: {
    label: string;
    value?: string;
    onChange?: (value: any) => void;
  }) => {
    return (
      <>
        <Col>{label}</Col>
        <Col>
          <ColorPickerPopover defaultValue={value} onSubmit={onChange}>
            <StyledColor color={value} />
          </ColorPickerPopover>
        </Col>
      </>
    );
  },
);

const InputNumberScope = memo(
  ({
    value,
    onChange,
  }: {
    value?: [number, number];
    onChange?: (value: any) => void;
  }) => {
    const [[min, max], setState] = useState<
      [number | undefined, number | undefined]
    >([undefined, undefined]);
    const [index, setIndex] = useState<number>(0);

    useEffect(() => {
      setIndex(index + 1);
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    useEffect(() => {
      if (Array.isArray(value)) {
        setState([Number(value[0]), Number(value[1])]);
      } else {
        setState([value, undefined]);
      }
    }, [value]);

    const inputNumberScopeChange = state => {
      setState(state);
      const result = state.filter(num => typeof num === 'number');
      if (result.length === 2) {
        onChange?.(state);
      } else {
        onChange?.(undefined);
      }
    };

    const minChange = (value: number) => inputNumberScopeChange([value, max]);
    const maxChange = (value: number) => inputNumberScopeChange([min, value]);

    return (
      <Row gutter={24} align="middle" key={index}>
        <Col>
          <InputNumber
            placeholder="最小值"
            defaultValue={min}
            onChange={minChange}
          />
        </Col>
        <Col>-</Col>
        <Col>
          <InputNumber
            placeholder="最大值"
            defaultValue={max}
            onChange={maxChange}
          />
        </Col>
      </Row>
    );
  },
);

const StyledColor = styled.div`
  position: relative;
  width: 16px;
  height: 16px;
  cursor: pointer;
  background-color: ${props => props.color};
  ::after {
    position: absolute;
    top: -7px;
    left: -7px;
    display: inline-block;
    width: 30px;
    height: 30px;
    content: '';
    border: 1px solid ${p => p.theme.borderColorBase};
    border-radius: 5px;
  }
`;

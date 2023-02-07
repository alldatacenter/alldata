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

import React, { useEffect, useState } from 'react';
import { AutoComplete, Button, Table, Input, InputNumber, Form } from 'antd';
import { FormItemProps } from 'antd/lib/form';
import { TableProps } from 'antd/lib/table';
import { useTranslation } from 'react-i18next';
import HighSelect from '@/components/HighSelect';
import { useUpdateEffect } from '@/hooks';
import isEqual from 'lodash/isEqual';
import styles from './index.module.less';

// Row data exposed to the outside
type RowValueType = Record<string, unknown>;

// Row data used internally by the current component
interface RecordType extends RowValueType {
  _etid: string;
}

type FormCompProps = Record<string, unknown>;

export interface ColumnsItemProps {
  title: string;
  dataIndex: string;
  initialValue?: unknown;
  width?: number;
  type?: 'text' | 'input' | 'inputnumber' | 'select' | 'autocomplete';
  // Props passed to form(input, select, ...)
  props?:
    | FormCompProps
    | ((val: unknown, rowVal: RowValueType, idx: number, isNew?: boolean) => FormCompProps);
  rules?: FormItemProps['rules'];
  // The value will be erased when invisible
  visible?: (val: unknown, rowVal: RowValueType) => boolean | boolean;
}

export interface EditableTableProps
  extends Omit<TableProps<any>, 'value' | 'onChange' | 'columns'> {
  // id comes from FormItem, like name
  id?: string;
  value?: RowValueType[];
  onChange?: (value: RowValueType[]) => void;
  columns: ColumnsItemProps[];
  // Can Edit(Can be changed to read-only)? Default: true.
  editing?: boolean;
  // If is not required, all rows can be delete. Default: true.
  required?: boolean;
  // Can remove a line? Default: true.
  canDelete?: boolean | ((rowVal: RowValueType, idx: number, isNew?: boolean) => boolean);
  // Can add a new line? Default: true.
  canAdd?: boolean;
}

const getRowInitialValue = (columns: EditableTableProps['columns']) =>
  columns.reduce(
    (acc, cur) => ({
      ...acc,
      [cur.dataIndex]: cur.initialValue,
    }),
    {
      _etid: `_etnew_${Math.random().toString()}`, // The tag of new.
    },
  );

const removeIdFromValues = (values: RecordType[]): RowValueType[] =>
  values.map(item => {
    const obj = { ...item };
    delete obj._etid;
    return obj;
  });

const addIdToValues = (values: RowValueType[]): RecordType[] =>
  values?.map(item => {
    const obj = { ...item };
    obj._etid = Math.random().toString();
    return obj as RecordType;
  });

const EditableTable = ({
  id,
  value,
  onChange,
  columns,
  editing = true,
  required = true,
  canDelete = true,
  canAdd = true,
  ...rest
}: EditableTableProps) => {
  if (!id) {
    console.error(
      'The id is lost, which may cause an error in the value of the array. Please check! Has the component library changed?',
    );
  }

  const { t } = useTranslation();

  const [data, setData] = useState<RecordType[]>(
    addIdToValues(value) || (required ? [getRowInitialValue(columns)] : []),
  );

  const [colsSet, setColsSet] = useState(new Set(columns.map(item => item.dataIndex)));

  useEffect(() => {
    if (value && !isEqual(value, removeIdFromValues(data))) {
      setData(addIdToValues(value));
    }
    // eslint-disable-next-line
  }, [value]);

  useUpdateEffect(() => {
    const newColsSet = new Set(columns.map(item => item.dataIndex));
    if (!isEqual(colsSet, newColsSet)) {
      const rowInitialValue = [getRowInitialValue(columns)];
      setColsSet(newColsSet);
      setData(rowInitialValue);
      triggerChange(rowInitialValue);
    }
    // eslint-disable-next-line
  }, [columns]);

  const triggerChange = (newData: RecordType[]) => {
    if (onChange) {
      onChange(removeIdFromValues(newData));
    }
  };

  const onAddRow = () => {
    const newRecord = getRowInitialValue(columns);
    newRecord._etid = `_etnew_${newRecord._etid}`; // The tag of new.
    const newData = data.concat([newRecord]);
    setData(newData);
    triggerChange(newData);
  };

  const onDeleteRow = ({ _etid }: RecordType) => {
    const newData = [...data];
    const index = newData.findIndex(item => item._etid === _etid);
    newData.splice(index, 1);
    setData(newData);
    triggerChange(newData);
  };

  const onTextChange = (object: Record<string, unknown>, { _etid }: RecordType) => {
    const newData = data.map(item => {
      if (item._etid === _etid) {
        return {
          ...item,
          ...object,
        };
      }
      return item;
    });
    setData(newData);
    triggerChange(newData);
  };

  let tableColumns = columns.map(item => ({
    title: item.title,
    dataIndex: item.dataIndex,
    width: item.width || 100,
    render: (text, record: RecordType, idx: number) => {
      if (!editing) {
        return text;
      } else if (
        typeof item.visible === 'function' ? !item.visible(text, record) : item.visible === false
      ) {
        return '-';
      }

      const props =
        typeof item.props === 'function'
          ? item.props(text, record, idx, record._etid?.indexOf('_etnew_') === 0)
          : item.props;

      const formCompObj = {
        input: (
          <Input
            {...props}
            value={text}
            onChange={e => onTextChange({ [item.dataIndex]: e.target.value }, record)}
          />
        ),
        inputnumber: (
          <InputNumber
            {...props}
            value={text}
            onChange={value => onTextChange({ [item.dataIndex]: value }, record)}
          />
        ),
        select: (
          <HighSelect
            dropdownMatchSelectWidth={false}
            {...props}
            value={text}
            onChange={(value, ...rest) => {
              onTextChange({ [item.dataIndex]: value }, record);
              if (props.onChange) {
                // onChange supports returning an object, triggering the change of value
                const result = (props.onChange as Function)(value, ...rest);
                if (result) onTextChange(result, record);
              }
            }}
          />
        ),
        autocomplete: (
          <AutoComplete
            dropdownMatchSelectWidth={false}
            {...props}
            value={text}
            onChange={(value, ...rest) => {
              onTextChange({ [item.dataIndex]: value }, record);
              if (props.onChange) {
                // onChange supports returning an object, triggering the change of value
                const result = (props.onChange as Function)(value, ...rest);
                if (result) onTextChange(result, record);
              }
            }}
          />
        ),
        text: <span>{text}</span>,
      };

      return (
        // The FormItem here is just borrowed to achieve the verification effect. The name borrows a built-in property (__proto__) to mount, so that the outer component will not traverse to this value when used
        // Use div to wrap input, select, etc. so that the value and onChange events are not taken over by FormItem
        // So the actual value change must be changed by onChange itself and then exposed to the outer component
        <Form.Item
          rules={item.rules?.map(rule =>
            typeof rule === 'function' ? rule : { ...rule, transform: () => text ?? '' },
          )}
          messageVariables={{ label: item.title }}
          // If the `name=[id, idx, item.dataIndex]` is used, the array value error will occur when the select/autocomplete input is entered, and the setValue will be automatically reset.
          name={['__proto__', 'editableRow', idx, item.dataIndex]}
          className={styles.formItem}
        >
          <div>{formCompObj[item.type || 'input']}</div>
        </Form.Item>
      );
    },
  }));

  if (editing) {
    tableColumns = tableColumns.concat({
      title: t('basic.Operating'),
      dataIndex: 'actions',
      width: 100,
      render: (text, record, idx) =>
        (required ? data.length !== 1 : true) &&
        (typeof canDelete === 'boolean'
          ? canDelete
          : canDelete(record, idx, record._etid?.indexOf('_etnew_') === 0)) && (
          <Button type="link" onClick={() => onDeleteRow(record)}>
            {t('basic.Delete')}
          </Button>
        ),
    } as any);
  }

  return (
    <Table
      {...rest}
      dataSource={data}
      columns={tableColumns}
      rowKey="_etid"
      footer={
        editing && canAdd
          ? () => (
              <Button type="link" style={{ padding: 0 }} onClick={onAddRow}>
                {t('components.EditableTable.NewLine')}
              </Button>
            )
          : null
      }
    />
  );
};

export default EditableTable;

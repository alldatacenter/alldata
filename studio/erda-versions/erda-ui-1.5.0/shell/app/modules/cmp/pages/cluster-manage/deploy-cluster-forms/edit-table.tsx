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
import { Icon as CustomIcon } from 'common';
import { FormInstance } from 'core/common/interface';
import { map } from 'lodash';
import { Form, Table, Popconfirm, Button, Input, Select } from 'antd';
import i18n from 'i18n';
import './edit-table.scss';

const { Option } = Select;

interface IEditableTableProps {
  form: FormInstance;
  columns: any[];
  data: any[];
  add: (...args: any) => void;
  del: (...args: any) => void;
  edit: (...args: any) => void;
}
export const EditableTable = (props: IEditableTableProps) => {
  const { columns, data, add, del, edit } = props;

  const fullColumns = [
    ...columns,
    {
      title: i18n.t('operation'),
      className: 'edit-operation',
      dataIndex: 'operation',
      width: 120,
      render: (text: any, rec: any) => (
        <Popconfirm title={`${i18n.t('confirm deletion')}?`} onConfirm={() => handleDelete(rec)}>
          <CustomIcon type="shanchu" />
        </Popconfirm>
      ),
    },
  ];

  const handleDelete = (rec: any) => {
    del(rec);
  };
  const handleAdd = () => {
    const newData = {
      ip: '',
      type: '',
      tag: '',
    };
    add(newData);
  };

  const handleSave = (row: any) => {
    edit(row);
  };

  const components = {
    body: {
      row: EditableFormRow,
      cell: EditableCell,
    },
  };

  const _columns = fullColumns.map((col: any) => {
    if (!col.editable) {
      return col;
    }
    return {
      ...col,
      onCell: (record: any) => ({
        record,
        editable: col.editable,
        dataIndex: col.dataIndex,
        type: col.type,
        options: col.options,
        title: col.title,
        rules: col.rules,
        handleSave,
      }),
    };
  });
  return (
    <div>
      <Button onClick={handleAdd} type="primary" style={{ marginBottom: 16 }}>
        {i18n.t('add')}
      </Button>
      <Table
        className="node-edit-table"
        components={components}
        rowClassName={() => 'editable-row'}
        bordered
        dataSource={data}
        columns={_columns}
        rowKey={(rec: any, i: number) => `${i}${rec.ip}`}
        pagination={false}
        scroll={{ x: '100%' }}
      />
    </div>
  );
};

interface ICellProps {
  record: any;
  children: any;
  dataIndex: string;
  title: string;
  editable: boolean;
  type?: string;
  options?: string[];
  index: number;
  rules?: any;
  handleSave: (args?: any) => void;
}
const EditableCell = (props: ICellProps) => {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const {
    record,
    handleSave,
    children,
    dataIndex,
    title,
    type,
    options,
    editable,
    index,
    rules = [],
    ...restProps
  } = props;
  const formRef = React.useRef(null);
  const inputRef = React.useRef(null);
  const save = (e: any) => {
    const curForm = formRef && (formRef.current as any);
    if (curForm) {
      curForm.validateFields().then((values: any) => {
        handleSave({ ...record, ...values });
        inputRef && inputRef.current && (inputRef.current as any).blur();
      });
    }
  };

  const renderCell = (form: FormInstance) => {
    formRef.current = form;
    let initialValue = record[dataIndex];
    if ((initialValue === '' || initialValue === undefined) && type === 'select') {
      initialValue = (options as any)[0];
    }
    return (
      <Form.Item
        name={dataIndex}
        style={{ margin: 0 }}
        initialValue
        rules={[
          {
            required: dataIndex !== 'tag',
            message: `${i18n.t('please enter')}${title}`,
          },
          ...rules,
        ]}
      >
        {type === 'select' ? (
          <Select size="small">
            {map(options, (val: string) => (
              <Option key={val} value={`${val}`}>
                {val}
              </Option>
            ))}
          </Select>
        ) : (
          <Input ref={inputRef} onPressEnter={save} onBlur={save} />
        )}
      </Form.Item>
    );
  };

  return (
    <td {...restProps}>{editable ? <EditableContext.Consumer>{renderCell}</EditableContext.Consumer> : children}</td>
  );
};

const EditableContext = React.createContext({ form: null });

const EditableRow = ({ form, index, ...props }: any) => (
  <EditableContext.Provider value={form}>
    <tr {...props} />
  </EditableContext.Provider>
);

const EditableFormRow = (props) => {
  const [form] = Form.useForm();
  return <EditableRow form {...props} />;
};

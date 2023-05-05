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

import { map, find, reject, uniqueId, isEqual } from 'lodash';
import React from 'react';
import { Form, Table, Input, Popconfirm, Button } from 'antd';
import { regRules } from 'common/utils';
import classNames from 'classnames';
import { FormInstance, ColumnProps } from 'core/common/interface';
import i18n from 'i18n';

import './index.scss';

const FormItem = Form.Item;
const inputRule = regRules.banFullWidthPunctuation;
const { TextArea } = Input;

const ROW_PREFIX = '_tb_';
const ROW_KEY = `${ROW_PREFIX}key`;
const ROW_VALUE = `${ROW_PREFIX}value`;
const noop = () => {};

interface IInputItem {
  form: FormInstance;
  nameId: string;
  name: string;
  value: string;
  existKeys?: string[];
  dataSource?: IItemData[];
  initialValue?: string;
  editDisabled?: boolean;
  isTextArea?: boolean;
  onChange: (e: React.ChangeEvent, name: string, nameId: string) => void;
  validate?: (rule: any, value: any, callback: Function) => void;
  maxLength?: number;
}
const InputItem = ({
  nameId,
  name,
  initialValue,
  onChange,
  dataSource,
  existKeys = [],
  editDisabled,
  isTextArea,
  validate,
  maxLength,
}: IInputItem) => {
  return (
    <FormItem
      name={name + nameId}
      rules={[
        {
          required: true,
          message: i18n.t('common:this item is required'),
        },
        inputRule,
        {
          validator: (rule, value, callback) => {
            if (dataSource) {
              const hasSameKey = dataSource.some(
                (item) => rule.field !== ROW_KEY + item.uniKey && item[ROW_KEY] === value,
              );
              const hasSameKeyOut = existKeys.includes(value);
              if (hasSameKey) {
                callback(i18n.t('common:key value must be unique'));
              } else if (hasSameKeyOut) {
                callback(i18n.t('common:this configuration already exists'));
              }
              validate && validate(rule, value, callback);
              callback();
            } else {
              callback();
            }
          },
        },
      ]}
      initialValue={initialValue}
    >
      {isTextArea ? (
        <TextArea autoSize onBlur={(e) => onChange(e, name, nameId)} disabled={editDisabled} maxLength={maxLength} />
      ) : (
        <Input
          placeholder={i18n.t('please enter')}
          size="default"
          onBlur={(e) => onChange(e, name, nameId)}
          disabled={editDisabled}
          maxLength={maxLength}
        />
      )}
    </FormItem>
  );
};

interface IItemData {
  uniKey: string;
  [prop: string]: string;
}
const convertToColumnData = (data?: object): IItemData[] => {
  return map(data, (v, k) => {
    return { [ROW_KEY]: String(k).trim(), [ROW_VALUE]: String(v).trim(), uniKey: uniqueId() };
  });
};

const convertToMapData = (arr: IItemData[]) => {
  const convertedObj = {};
  arr.forEach((item) => {
    convertedObj[item[ROW_KEY].trim()] = item[ROW_VALUE].trim();
  });
  return convertedObj;
};

interface IProps {
  data?: object | IItemData[];
  form: FormInstance;
  title?: string | React.ReactNode;
  pagination?: {
    pageSize: number;
    current?: number;
  };
  className?: string;
  addBtnText?: string;
  disableAdd?: boolean;
  disableDelete?: boolean;
  editDisabled?: boolean;
  isTextArea: boolean;
  keyDisabled?: boolean;
  existKeys?: string[];
  onDel?: (data: object | IItemData[]) => void;
  onChange?: (data: object | IItemData[]) => void;
  validate?: {
    key: (rule: any, value: any, callback: Function) => void;
    value: (rule: any, value: any, callback: Function) => void;
  };
  maxLength?: number;
}

interface IState {
  dataSource: IItemData[];
  preData: object | null;
}
class KeyValueTable extends React.Component<IProps, IState> {
  // 供外部处理数据
  static dealTableData(data: object, as?: string) {
    const tbData = {};
    const otherData = {};
    map(data, (value, key) => {
      if (key.includes(ROW_VALUE)) {
        return;
      }
      const [, index] = key.split(ROW_KEY);
      if (index) {
        tbData[data[key]] = data[ROW_VALUE + index];
      } else {
        otherData[key] = value;
      }
    });
    return as ? { ...otherData, [as]: { ...tbData } } : { ...otherData, ...tbData };
  }

  static getDerivedStateFromProps(props: IProps, preState: IState): Partial<IState> | null {
    if (!isEqual(props.data, preState.preData)) {
      return {
        dataSource: Array.isArray(props.data) ? props.data : convertToColumnData(props.data),
        preData: props.data,
      };
    }
    return null;
  }

  table: any;

  constructor(props: IProps) {
    super(props);
    this.state = {
      dataSource: Array.isArray(props.data) ? props.data : convertToColumnData(props.data),
      preData: null,
    };
  }

  getTableData() {
    return convertToMapData(this.state.dataSource);
  }

  handleAdd = () => {
    const { dataSource } = this.state;
    const {
      form: { validateFields },
    } = this.props;

    // 回到分页的第一页
    if (this.table) {
      this.table.setState({
        pagination: {
          current: 1,
        },
      });
    }

    return validateFields().then(() => {
      // 分页非第一页时，判断下第一个值不为空后添加新行
      const canAdd = dataSource.length === 0 || (dataSource[0][ROW_KEY] && dataSource[0][ROW_VALUE]);
      if (canAdd) {
        const newData = { [ROW_KEY]: '', [ROW_VALUE]: '', uniKey: uniqueId() };
        this.setState({ dataSource: [newData, ...dataSource] });
      }
    });
  };

  handleItemChange = (e: React.ChangeEvent<HTMLInputElement>, name: string, uniKey: string) => {
    const { value } = e.target;
    const dataSource = [...this.state.dataSource];
    const { onChange } = this.props;
    (find(dataSource, { uniKey }) as object)[name] = value;
    this.setState({ dataSource });
    onChange && onChange(Array.isArray(this.props.data) ? dataSource : convertToMapData(dataSource));
  };

  handleDelete = (uniKey: string) => {
    const dataSource = [...this.state.dataSource];
    const onDel = this.props.onDel || noop;
    this.setState(
      {
        dataSource: reject(dataSource, { uniKey }),
      },
      () => {
        onDel(Array.isArray(this.props.data) ? this.state.dataSource : this.getTableData());
      },
    );
  };

  render() {
    const { dataSource } = this.state;
    const {
      form,
      title = '',
      pagination = { pageSize: 5, hideOnSinglePage: true },
      className = '',
      addBtnText = i18n.t('common:add'),
      disableAdd = false,
      disableDelete = false,
      editDisabled,
      existKeys = [],
      keyDisabled = false,
      isTextArea,
      validate,
      maxLength,
    } = this.props;
    const showPagination = dataSource.length >= pagination.pageSize;

    const deleteBtnClass = classNames({
      'delete-row-btn': true,
      'table-operations': true,
      disabled: editDisabled,
    });

    const columns: Array<ColumnProps<IItemData>> = [
      {
        title: 'KEY',
        dataIndex: 'key',
        width: 280,
        ellipsis: false,
        render: (text: string, record: IItemData) => (
          <InputItem
            form={form}
            name={ROW_KEY}
            value={text}
            nameId={record.uniKey}
            dataSource={dataSource}
            existKeys={existKeys}
            onChange={this.handleItemChange}
            initialValue={record[ROW_KEY]}
            editDisabled={editDisabled || keyDisabled}
            validate={validate && validate.key}
            maxLength={maxLength}
          />
        ),
      },
      {
        title: 'VALUE',
        dataIndex: 'value',
        ellipsis: false,
        render: (text: string, record: IItemData) => (
          <InputItem
            form={form}
            name={ROW_VALUE}
            value={text}
            nameId={record.uniKey}
            onChange={this.handleItemChange}
            initialValue={record[ROW_VALUE]}
            editDisabled={editDisabled}
            isTextArea={isTextArea}
            validate={validate && validate.value}
            maxLength={maxLength}
          />
        ),
      },
    ];

    if (!disableDelete) {
      columns.push({
        title: i18n.t('operation'),
        width: 160,
        dataIndex: 'operation',
        className: 'operation',
        render: (_text, record) => {
          return (
            <Popconfirm
              title={`${i18n.t('common:confirm to delete')}?`}
              onConfirm={() => this.handleDelete(record.uniKey)}
            >
              <span className={deleteBtnClass}>
                <span className="table-operations-btn">{i18n.t('delete')}</span>
              </span>
            </Popconfirm>
          );
        },
      });
    }

    return (
      <div className="key-value-table-wrap">
        <div className="add-row-btn-wrap">
          {!disableAdd && (
            <Button size="small" type="primary" ghost disabled={editDisabled} onClick={this.handleAdd}>
              {addBtnText}
            </Button>
          )}
          <span>{title}</span>
        </div>
        <Table
          dataSource={dataSource}
          columns={columns}
          rowKey="uniKey"
          pagination={showPagination ? pagination : false}
          className={`key-value-table ${className}`}
          ref={(ref) => {
            this.table = ref;
          }}
          scroll={undefined}
        />
      </div>
    );
  }
}

export default KeyValueTable;

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
import KeyValueTextArea from '../key-value-textarea';
import KeyValueTable from '../key-value-table';
import { Radio } from 'antd';
import { isEqual } from 'lodash';
import { FormInstance } from 'core/common/interface';
import i18n from 'i18n';
import './index.scss';

const trim = (str: string) => str.replace(/^\s+|\s+$/g, '');
const convertToTextData = (data: object) => Object.keys(data || {}).reduce((all, k) => `${all}${k}: ${data[k]}\n`, '');
export const convertTextToMapData = (value: string) =>
  value
    .split('\n')
    .filter((row) => row.length > 0)
    .reduce((obj, r) => {
      const [k, ...v] = r.split(':');
      if (v.length) {
        obj[trim(k)] = trim(v.join(':'));
      }
      return obj;
    }, {});

/**
 *
 * 参数项:
 *  tableProps: kv-table 参数集合
 *  textAreaProps: kv-textArea 参数集合
 *
 * Usage:
 *  <KeyValueEditor
 *    dataSource={dataSource}
 *    form={form}
 *    tableProps={tableProps}
 *    textAreaProps={textAreaProps}
 *  />
 *
 */

interface IProps {
  dataSource?: object;
  tableProps?: object;
  textAreaProps?: object;
  editDisabled?: boolean;
  isNeedTextArea?: boolean;
  isValueTextArea?: boolean;
  keyDisabled?: boolean;
  disableAdd?: boolean;
  disableDelete?: boolean;
  existKeys?: string[];
  form: FormInstance;
  onChange?: (data: object) => void;
  title?: string | React.ReactNode;
  validateField?: {
    table: {
      key: (rule: any, value: any, callback: Function) => void;
      value: (rule: any, value: any, callback: Function) => void;
    };
    text: (rule: any, value: { k: string; v: string }, callback: Function) => void;
  };
  maxLength?: number;
}

interface IState {
  dataSource: object;
  tableMode: boolean;
  prevDataSource: any;
}
class KeyValueEditor extends React.Component<IProps, IState> {
  static getDerivedStateFromProps({ dataSource }: IProps, prevState: IState) {
    if (!isEqual(dataSource, prevState.prevDataSource)) {
      return { ...prevState, prevDataSource: dataSource, dataSource };
    }
    return null;
  }

  private table: KeyValueTable | null;

  private textArea: KeyValueTextArea | null;

  constructor(props: IProps) {
    super(props);

    this.state = {
      dataSource: props.dataSource || {},
      tableMode: true,
      prevDataSource: undefined,
    };
  }

  getEditData = () => {
    const { tableMode } = this.state;
    const data = tableMode
      ? (this.table as KeyValueTable).getTableData()
      : convertTextToMapData((this.textArea as KeyValueTextArea).getTextData());
    return data;
  };

  toggleEditMode = () =>
    new Promise((resolve) => {
      const {
        form: { validateFields },
      } = this.props;
      validateFields()
        .then(() => {
          const nowIsTableMode = this.state.tableMode;
          if (nowIsTableMode) {
            this.setState({
              dataSource: (this.table as KeyValueTable).getTableData(),
              tableMode: !nowIsTableMode,
            });
          } else {
            this.setState({
              dataSource: convertTextToMapData((this.textArea as KeyValueTextArea).getTextData()),
              tableMode: !nowIsTableMode,
            });
          }
          resolve({ mode: !nowIsTableMode ? 'key-value' : 'text' });
        })
        .catch(({ errorFields }: { errorFields: Array<{ name: any[]; errors: any[] }> }) => {
          resolve(errorFields);
        });
    });

  render() {
    const { dataSource, tableMode } = this.state;
    const {
      title,
      form,
      tableProps,
      textAreaProps,
      editDisabled,
      existKeys = [],
      onChange,
      isNeedTextArea = true,
      keyDisabled,
      disableAdd,
      isValueTextArea = false,
      disableDelete,
      validateField,
      maxLength,
    } = this.props;
    const tableData = dataSource;
    const textData = convertToTextData(dataSource);
    return (
      <div>
        <div className="flex justify-between items-center mb-3">
          <span className="key-value-title">{title || i18n.t('default:information configuration')}</span>
          {isNeedTextArea ? (
            <Radio.Group size="small" value={tableMode ? 'key-value' : 'text'} onChange={this.toggleEditMode}>
              <Radio.Button value="text">{i18n.t('default:text mode')}</Radio.Button>
              <Radio.Button value="key-value">{i18n.t('default:entry mode')}</Radio.Button>
            </Radio.Group>
          ) : null}
        </div>
        {tableMode ? (
          <KeyValueTable
            data={tableData}
            editDisabled={editDisabled}
            keyDisabled={keyDisabled}
            existKeys={existKeys}
            form={form}
            disableAdd={disableAdd}
            disableDelete={disableDelete}
            addBtnText={i18n.t('common:add parameter')}
            title={null}
            ref={(ref) => {
              this.table = ref;
            }}
            // 该组件认为删除 input-item 和改变 input-item 的值是同一种行为
            onChange={onChange}
            onDel={onChange}
            isTextArea={isValueTextArea}
            validate={validateField ? validateField.table : undefined}
            maxLength={maxLength}
            {...tableProps}
          />
        ) : (
          <div>
            {/* <div className="mb-4" style={{ display: 'flex', flexDirection: 'row-reverse', lineHeight: '28px' }}> */}
            {/*  {modeSwitch} */}
            {/* </div> */}
            <KeyValueTextArea
              data={textData}
              editDisabled={editDisabled}
              existKeys={existKeys}
              form={form}
              ref={(ref) => {
                this.textArea = ref;
              }}
              validate={validateField ? validateField.text : undefined}
              maxLength={maxLength}
              {...textAreaProps}
            />
          </div>
        )}
      </div>
    );
  }
}

export default KeyValueEditor;

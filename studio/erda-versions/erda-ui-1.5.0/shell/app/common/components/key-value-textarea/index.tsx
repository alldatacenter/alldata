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
import { Input, Form } from 'antd';
import { regRules } from 'common/utils';
import { FormInstance } from 'core/common/interface';
import i18n from 'i18n';
import './index.scss';

const { TextArea } = Input;
const FormItem = Form.Item;
const { pattern } = regRules.banFullWidthPunctuation;

const trim = (str: string) => str.replace(/^\s+|\s+$/g, '');
const errMsgMap = (rowNum: number, type: string, maxLength?: number) => {
  let errMsg = '';
  switch (type) {
    case 'l_key':
      errMsg = `${i18n.t('common:missing')} key`;
      break;
    case 'l_value':
      errMsg = `${i18n.t('common:missing')} value`;
      break;
    case 'l_colon':
      errMsg = i18n.t('common:lack of english colon');
      break;
    case 'n_unique':
      errMsg = `key ${i18n.t('common:must be unique')}`;
      break;
    case 'n_valid':
      errMsg = i18n.t('common:full-width punctuation not allowed');
      break;
    case 'too_long_value':
      errMsg = i18n.t('the length of {type} must not exceed {maxLength} characters', { type: 'Value', maxLength });
      break;
    case 'too_long_key':
      errMsg = i18n.t('the length of {type} must not exceed {maxLength} characters', { type: 'Key', maxLength });
      break;
    default:
      return null;
  }
  return `第${rowNum}行: ${errMsg}`;
};

interface IProps {
  data: string;
  fieldName?: 'kv-text';
  form: FormInstance;
  className?: string;
  autoSize?: object;
  rows?: number;
  placeholder?: string;
  editDisabled?: boolean;
  existKeys?: string[];
  validate?: (rule: any, value: { k: string; v: string }, callback: Function) => void;
  maxLength?: number;
}
interface IState {
  textData: string;
}
class KeyValueTextArea extends React.Component<IProps, IState> {
  constructor(props: IProps) {
    super(props);

    this.state = {
      textData: props.data,
    };
  }

  handleTextChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const text = e.target.value;
    this.setState({ textData: text });
  };

  getTextData = () => {
    return this.state.textData;
  };

  onValidator = (rule: string, value: string, callback: (b: string | undefined) => void) => {
    const { validate, maxLength } = this.props;
    let errMsg;
    const keys: string[] = [];
    value
      .split('\n')
      .filter((row: string) => row.length > 0)
      .forEach((row: string, i: number) => {
        const [k, ..._v] = row.split(':');
        const v = _v.join(':');
        const rowNum = i + 1;
        if (!v.length) {
          errMsg = errMsgMap(rowNum, 'l_colon');
        } else if (!trim(v).length) {
          errMsg = errMsgMap(rowNum, 'l_value');
        } else if (typeof maxLength !== 'undefined' && trim(v).length > maxLength) {
          errMsg = errMsgMap(rowNum, 'too_long_value', maxLength);
        }
        if (trim(k) === '') {
          errMsg = errMsgMap(rowNum, 'l_key');
        } else if (typeof maxLength !== 'undefined' && trim(k).length > maxLength) {
          errMsg = errMsgMap(rowNum, 'too_long_key', maxLength);
        } else if (keys.includes(k)) {
          errMsg = errMsgMap(rowNum, 'n_unique');
        } else if ((this.props.existKeys || []).includes(k)) {
          callback(`${rowNum}${i18n.t('common:line')}: ${i18n.t('common:this configuration already exists')}`);
        }
        validate && validate(rule, { k, v }, callback);

        const isValueEmpty = v[0] === '' || v.length === 0;
        if (!pattern.test(k) || (!isValueEmpty && !pattern.test(v))) {
          errMsg = errMsgMap(rowNum, 'n_valid');
        }
        keys.push(k);
      });
    callback(errMsg);
  };

  render() {
    const {
      fieldName = 'kv-text',
      className = '',
      autoSize = { minRows: 10, maxRows: 15 },
      rows,
      placeholder = `${i18n.t('common:please input')} key: value ${i18n.t('common:form of text')}`,
      editDisabled,
    } = this.props;
    const { textData } = this.state;

    return (
      <div className="key-value-textarea-wrap">
        <FormItem
          name={fieldName}
          initialValue={textData}
          rules={[
            {
              validator: this.onValidator,
            },
          ]}
        >
          <TextArea
            className={className}
            disabled={editDisabled}
            rows={rows}
            placeholder={placeholder}
            autoSize={autoSize}
            onChange={this.handleTextChange}
          />
        </FormItem>
      </div>
    );
  }
}

export default KeyValueTextArea;

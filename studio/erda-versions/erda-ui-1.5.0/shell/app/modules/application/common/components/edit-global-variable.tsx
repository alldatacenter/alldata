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

import { FormComponentProps, FormInstance } from 'core/common/interface';
import React, { PureComponent } from 'react';
import { Form, Button } from 'antd';
import { cloneDeep, forEach, findIndex, uniqueId } from 'lodash';
import VariableInputGroup from './variable-input-group';
import i18n from 'i18n';
import { ErdaIcon } from 'common';

const { Item } = Form;

interface IEditGlobalVariableProps {
  globalVariable: any;
  editing: boolean;
  onSubmit: (globalVariable: any) => void;
}

const convertGlobalVariableList = (globalVariable: any) => {
  const list: any[] = [];
  forEach(globalVariable, (value: any, key: string) => {
    list.push({
      key,
      value,
    });
  });

  return list;
};

class EditGlobalVariable extends PureComponent<IEditGlobalVariableProps & FormComponentProps, any> {
  formRef = React.createRef<FormInstance>();

  state = {
    globalVariableList: [],
  };

  UNSAFE_componentWillMount(): void {
    const list = convertGlobalVariableList(this.props.globalVariable);
    this.setState({
      globalVariableList: list.map((i: any) => ({
        ...i,
        id: uniqueId('var-'),
      })),
    });
  }

  render() {
    const { globalVariableList } = this.state;
    const { editing } = this.props;

    const content = globalVariableList.map((item: any) => {
      const input = <VariableInputGroup lock={false} disabled={!editing} onDelete={this.deleteVariable} />;
      return (
        <Item
          className="mr-0"
          key={item.key}
          name={item.id}
          initialValue={item}
          rules={[
            {
              required: true,
              message: i18n.t('dop:environment variables cannot be empty'),
            },
          ]}
        >
          {input}
        </Item>
      );
    });

    return (
      <Form ref={this.formRef} className="global-input-form">
        <div className="global-input-form-title">
          {i18n.t('dop:global environment variable')}
          {editing ? (
            <ErdaIcon
              type="plus"
              className="variable-icon cursor-pointer"
              onClick={this.addNewVariable}
            />
          ) : null}
        </div>
        {content}
        <div className="mt-3">
          {editing ? (
            <Button type="primary" ghost onClick={this.onSubmit}>
              {i18n.t('save')}
            </Button>
          ) : null}
        </div>
      </Form>
    );
  }

  private deleteVariable = (key: string) => {
    const { globalVariableList } = this.state;

    const index = findIndex(globalVariableList, (item: any) => item.key === key);
    globalVariableList.splice(index, 1);

    this.setState({
      globalVariableList: cloneDeep(globalVariableList),
    });
  };

  private onSubmit = () => {
    const { onSubmit } = this.props;
    const form = this.formRef.current;

    form
      ?.validateFields()
      .then((values: any) => {
        const object = {};
        forEach(values, (item: any, originKey: string) => {
          if (item.key !== '') {
            object[item.key] = item.value;
          } else {
            form?.setFields({
              [originKey]: {
                value: item,
                errors: [new Error(i18n.t('dop:environment variables cannot be empty'))],
              },
            });
          }
        });
        onSubmit(object);
      })
      .catch(({ errorFields }: { errorFields: Array<{ name: any[]; errors: any[] }> }) => {
        form?.scrollToField(errorFields[0].name);
      });
  };

  private addNewVariable = () => {
    const { globalVariableList } = this.state;

    globalVariableList.push({
      id: uniqueId('var-'),
      key: 'key',
      value: 'value',
    });
    this.setState({
      globalVariableList: cloneDeep(globalVariableList),
    });
  };
}

export default EditGlobalVariable;

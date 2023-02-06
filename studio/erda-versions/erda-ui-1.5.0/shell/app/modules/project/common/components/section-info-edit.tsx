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
import { FormModal, ConfigLayout } from 'common';
import { Form, Button, Avatar } from 'antd';
import { isEmpty, concat, get, forEach, isFunction } from 'lodash';
import Markdown from 'common/utils/marked';
import i18n from 'i18n';

interface IProps {
  data: any;
  hasAuth: boolean;
  name?: string | React.ReactNode;
  desc?: string | React.ReactNode;
  formName?: string;
  fieldsList: IField[] | ((arg?: any) => IField[]);
  readonlyForm?: JSX.Element;
  extraSections?: any[];
  updateInfo: (data: any) => Promise<any> | void;
  setCanGetClusterListAndResources?: (value: boolean) => void;
}
interface IField {
  label?: string;
  name?: string;
  viewType?: string;
  itemProps?: IItemProps;
  showInfo?: boolean; // type为hidden时也显示
  hideWhenReadonly?: boolean; // readonly状态下隐藏
  type?: string;
  options?: Function | any[];
  customRender?: (value: any) => any;
  getComp?: (o?: any) => any;
}
interface IState {
  modalVisible: boolean;
}
interface IItemProps {
  [proName: string]: any;
  type?: string;
}
const FormItem = Form.Item;
class SectionInfoEdit extends React.Component<IProps, IState> {
  state = { modalVisible: false };

  toggleModal = () => {
    this.setState({ modalVisible: !this.state.modalVisible });
    this.props.setCanGetClusterListAndResources?.(!this.state.modalVisible);
  };

  handleSubmit = (values: object) => {
    return Promise.resolve(this.props.updateInfo(values)).then(() => {
      this.toggleModal();
    });
  };

  getReadonlyInfo = () => {
    const { fieldsList, data } = this.props;
    const readonlyView: JSX.Element[] = [];
    const fList = typeof fieldsList === 'function' ? fieldsList() : fieldsList;
    forEach(fList, (item, index) => {
      const {
        itemProps,
        label,
        name,
        viewType,
        customRender,
        getComp,
        showInfo,
        type,
        options = [],
        hideWhenReadonly,
      } = item;

      if (!hideWhenReadonly && !(itemProps?.type === 'hidden' && !showInfo)) {
        let value = name === undefined && getComp ? getComp({ readOnly: true }) : get(data, name || '');
        if (type === 'radioGroup' && !isFunction(options)) {
          value = ((options as any[]).find((option) => option.value === value) || {}).name;
        }
        if (value !== undefined && value !== null && value !== '') {
          if (viewType === 'image') {
            readonlyView.push(
              <FormItem label={label} key={index}>
                <Avatar shape="square" src={value} size={100} />
              </FormItem>,
            );
          } else if (viewType === 'markdown') {
            readonlyView.push(
              <FormItem label={label} key={index}>
                {/* eslint-disable-next-line react/no-danger */}
                <p className="md-content" dangerouslySetInnerHTML={{ __html: Markdown(value) }} />
              </FormItem>,
            );
          } else if (type === 'select') {
            const objOptions = isFunction(options) ? options() : options;
            const target = objOptions.find((o: { value: string }) => o.value === value) || {};
            readonlyView.push(
              <FormItem label={label} key={index}>
                <p>{target.name || ''}</p>
              </FormItem>,
            );
          } else if (type === 'checkbox') {
            readonlyView.push(
              <FormItem label={label} key={index}>
                <p>{String(value)}</p>
              </FormItem>,
            );
          } else {
            readonlyView.push(
              <FormItem label={label} key={index}>
                <div>{customRender ? customRender(value) : value}</div>
              </FormItem>,
            );
          }
        }
      }
    });
    return (
      <Form layout="vertical" className="section-info-form">
        {readonlyView}
      </Form>
    );
  };

  render() {
    const { modalVisible } = this.state;
    const { data, fieldsList, hasAuth, readonlyForm, name, desc, formName, extraSections } = this.props;
    let sectionList = [
      {
        title: name,
        desc,
        titleOperate: hasAuth ? (
          <Button type="primary" ghost onClick={this.toggleModal}>
            {i18n.t('edit')}
          </Button>
        ) : null,
        children: readonlyForm || this.getReadonlyInfo(),
      },
    ];
    if (!isEmpty(extraSections)) {
      sectionList = concat(sectionList, extraSections);
    }
    return (
      <React.Fragment>
        <ConfigLayout sectionList={sectionList} />
        <FormModal
          width={700}
          name={formName || name || ''}
          formData={data}
          fieldsList={fieldsList}
          visible={modalVisible}
          onOk={this.handleSubmit}
          onCancel={this.toggleModal}
          modalProps={{ destroyOnClose: true }}
        />
      </React.Fragment>
    );
  }
}

export { SectionInfoEdit };

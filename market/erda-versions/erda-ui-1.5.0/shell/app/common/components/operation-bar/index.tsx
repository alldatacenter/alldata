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
import { Button } from 'antd';
import { RenderForm } from 'common';
import { FormInstance } from 'core/common/interface';
import i18n from 'i18n';
import './index.scss';

interface IProps {
  searchList: any[];
  onUpdateOps?: (opsVal: any) => void;
}

interface IState {
  [key: string]: any;
}
/**
 * usage:
 * <OperationBar searchList={searchListOps.list} onReset={searchListOps.onReset} onSubmit={searchListOps.onSubmit} />
 */

class OperationBar extends React.PureComponent<IProps, IState> {
  constructor(props: IProps) {
    super(props);
    this.state = {};
  }

  handleSubmit = (form: FormInstance) => {
    const formValue = form.getFieldsValue();
    this.props.onUpdateOps && this.props.onUpdateOps(formValue);
  };

  handleReset = (event: any, form: FormInstance) => {
    event.currentTarget.blur();
    form.resetFields();
    this.handleSubmit(form);
  };

  getFormInitialValue = (formType: string) => {
    let type = null;
    switch (formType) {
      default:
        type = undefined;
    }
    return type;
  };

  formatSearchList = () => {
    const searchList = this.props.searchList.map((search) => {
      return {
        required: false,
        initialValue: this.getFormInitialValue(search.type),
        extraProps: {
          colon: false,
          required: false,
        },
        ...search,
      };
    });
    searchList.push({
      getComp: ({ form }: { form: FormInstance }) => (
        <React.Fragment>
          <Button
            className="ops-bar-btn ops-bar-default-btn ops-bar-reset-btn"
            type="default"
            onClick={(e: any) => this.handleReset(e, form)}
          >
            {i18n.t('reset')}
          </Button>
          <Button className="ops-bar-btn" type="primary" ghost onClick={() => this.handleSubmit(form)}>
            {i18n.t('search')}
          </Button>
        </React.Fragment>
      ),
    });

    return searchList;
  };

  // resetOps = () => {
  //   this.props.searchList.forEach((search) => {
  //     this.setState({ [search.key]: undefined });
  //   });
  // };

  render() {
    const { searchList } = this.props;
    if (!searchList.length) {
      return null;
    }

    return (
      <div className="ops-bar-line">
        <RenderForm layout="inline" {...this.props} list={this.formatSearchList()} />
      </div>
    );
  }
}

export default OperationBar;
